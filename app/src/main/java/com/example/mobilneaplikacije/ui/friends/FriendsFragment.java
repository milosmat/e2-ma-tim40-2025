package com.example.mobilneaplikacije.ui.friends;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.UserPublic;
import com.example.mobilneaplikacije.data.repository.FriendsRepository;
import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.example.mobilneaplikacije.data.model.AllianceInvite;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    private FriendsRepository repo;
    private AllianceRepository allianceRepo;
    private EditText etSearch;
    private androidx.recyclerview.widget.RecyclerView rv;
    private FriendsAdapter adapter;
    private TextView tvAllianceInfo;
    private View btnCreateAlliance, btnInviteAlliance, btnLeaveAlliance, btnDisbandAlliance, btnAllianceDetails;
    private String currentAllianceId;
    private final List<UserPublic> friends = new ArrayList<>();
    private final List<UserPublic> results = new ArrayList<>();
    private final List<UserPublic> incoming = new ArrayList<>();
    private final List<AllianceInvite> allianceInvites = new ArrayList<>();

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) startScanQr();
                else Toast.makeText(getContext(), "Kamera potrebna za skeniranje.", Toast.LENGTH_SHORT).show();
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friends, container, false);
        repo = new FriendsRepository();
        allianceRepo = new AllianceRepository();

        etSearch = v.findViewById(R.id.etSearch);
        rv = v.findViewById(R.id.rvFriends);
        tvAllianceInfo = v.findViewById(R.id.tvAllianceInfo);
        btnCreateAlliance = v.findViewById(R.id.btnCreateAlliance);
        btnInviteAlliance = v.findViewById(R.id.btnInviteAlliance);
        btnLeaveAlliance = v.findViewById(R.id.btnLeaveAlliance);
        btnDisbandAlliance = v.findViewById(R.id.btnDisbandAlliance);
        btnAllianceDetails = v.findViewById(R.id.btnAllianceDetails);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        adapter = new FriendsAdapter(new FriendsAdapter.ActionHandler() {
            @Override public void onAccept(UserPublic u) { onIncomingAction(u); }
            @Override public void onAdd(UserPublic u) { onSearchAction(u); }
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnSearch).setOnClickListener(view -> doSearch());
        v.findViewById(R.id.btnShowMyQr).setOnClickListener(view -> showMyQr());
        v.findViewById(R.id.btnScanQr).setOnClickListener(view -> checkCameraThenScan());

        btnCreateAlliance.setOnClickListener(view -> promptCreateAlliance());
        btnInviteAlliance.setOnClickListener(view -> inviteFriendsToAlliance());
        btnLeaveAlliance.setOnClickListener(view -> leaveAlliance());
        btnDisbandAlliance.setOnClickListener(view -> disbandAlliance());
        btnAllianceDetails.setOnClickListener(view -> openAllianceDetails());

        return v;
    }

    @Override public void onResume() { super.onResume(); loadFriends(); loadIncoming(); loadAllianceInfo(); loadAllianceInvites(); }

    private void loadAllianceInvites() {
        allianceRepo.listIncomingInvites(new AllianceRepository.Callback<List<AllianceInvite>>() {
            @Override public void onSuccess(List<AllianceInvite> data) {
                allianceInvites.clear();
                allianceInvites.addAll(data);
                showAllianceInvites();
            }
            @Override public void onError(Exception e) {
                showAllianceInvites();
            }
        });
    }

    private void showAllianceInvites() {
        View root = getView();
        if (root == null) return;
        LinearLayout container = root.findViewById(R.id.listAllianceInvites);
        TextView title = root.findViewById(R.id.tvAllianceInvitesTitle);
        if (container == null || title == null) return;
        container.removeAllViews();
        if (allianceInvites.isEmpty()) {
            title.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            return;
        }
        title.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);
        LayoutInflater infl = LayoutInflater.from(getContext());
        for (AllianceInvite inv : allianceInvites) {
            View row = infl.inflate(R.layout.row_alliance_invite, container, false);
            TextView tv = row.findViewById(R.id.tvInviteText);
            String name = inv.allianceName != null ? inv.allianceName : inv.allianceId;
            tv.setText("Poziv u savez: " + (name != null ? name : "(nepoznat)"));
            row.findViewById(R.id.btnAcceptInvite).setOnClickListener(v -> respondInvite(inv, true));
            row.findViewById(R.id.btnDeclineInvite).setOnClickListener(v -> respondInvite(inv, false));
            container.addView(row);
        }
    }

    private void respondInvite(AllianceInvite inv, boolean accept) {
        allianceRepo.respondToInvite(inv.allianceId, accept, new AllianceRepository.Callback<Void>() {
            @Override public void onSuccess(Void data) {
                if (accept) Toast.makeText(getContext(), "Pridruzili ste se savezu", Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(), "Poziv odbijen", Toast.LENGTH_SHORT).show();
                loadAllianceInfo();
                loadAllianceInvites();
            }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void loadAllianceInfo() {
        allianceRepo.getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(AllianceRepository.AllianceInfo info) {
                currentAllianceId = info.allianceId;
                if (info.allianceId == null || info.allianceId.isEmpty() || info.alliance == null) {
                    tvAllianceInfo.setText("Niste u savezu.");
                    btnCreateAlliance.setEnabled(true);
                    btnInviteAlliance.setEnabled(false);
                    btnLeaveAlliance.setEnabled(false);
                    btnDisbandAlliance.setEnabled(false);
                    btnAllianceDetails.setEnabled(false);
                    return;
                }
                tvAllianceInfo.setText("U savezu: " + info.alliance.name + (info.isLeader ? " (vođa)" : ""));
                btnCreateAlliance.setEnabled(false);
                btnInviteAlliance.setEnabled(info.isLeader);
                btnLeaveAlliance.setEnabled(!info.isLeader);
                btnDisbandAlliance.setEnabled(info.isLeader);
                btnAllianceDetails.setEnabled(true);
            }
            @Override public void onError(Exception e) {
                tvAllianceInfo.setText("Greska: " + e.getMessage());
            }
        });
    }

    private void loadFriends() {
        repo.listFriends(new FriendsRepository.Callback<List<UserPublic>>() {
            @Override public void onSuccess(List<UserPublic> data) {
                friends.clear(); friends.addAll(data);
                refreshAdapter();
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(), "Greska: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            results.clear();
            refreshAdapter();
            return;
        }
        repo.searchUsersByUsername(query, new FriendsRepository.Callback<List<UserPublic>>() {
            @Override public void onSuccess(List<UserPublic> data) {
                results.clear();
                results.addAll(data);
                refreshAdapter();
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(), "Greska: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadIncoming() {
        repo.listIncomingRequests(new FriendsRepository.Callback<List<UserPublic>>() {
            @Override public void onSuccess(List<UserPublic> data) {
                incoming.clear();
                incoming.addAll(data);
                refreshAdapter();
            }
            @Override public void onError(Exception e) {  }
        });
    }

    private void onSearchAction(UserPublic u) {
        if ("incoming".equals(u.status)) {
            repo.acceptRequest(u.uid, new FriendsRepository.Callback<Void>() {
                @Override public void onSuccess(Void v) {
                    Toast.makeText(getContext(), "Zahtev prihvacen", Toast.LENGTH_SHORT).show();
                    doSearch(); loadFriends();
                }
                @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
            });
        } else if ("friend".equals(u.status)) {
        } else {
            repo.sendFriendRequest(u.uid, new FriendsRepository.Callback<Void>() {
                @Override public void onSuccess(Void v) {
                    Toast.makeText(getContext(), "Zahtev poslat", Toast.LENGTH_SHORT).show();
                    doSearch();
                }
                @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
            });
        }
    }

    private void onIncomingAction(UserPublic u) {
        repo.acceptRequest(u.uid, new FriendsRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                Toast.makeText(getContext(), "Zahtev prihvacen", Toast.LENGTH_SHORT).show();
                loadIncoming();
                loadFriends();
            }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void refreshAdapter() {
        adapter.setData(incoming, friends, results);
    }

    private void showMyQr() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyQrFragment())
                .addToBackStack(null)
                .commit();
    }

    private void checkCameraThenScan() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanQr();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanQr() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ScanQrFragment())
                .addToBackStack(null)
                .commit();
    }

    private void promptCreateAlliance() {
        EditText input = new EditText(getContext());
        input.setHint("Naziv saveza");
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Kreiraj savez")
                .setView(input)
                .setPositiveButton("Kreiraj", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(getContext(), "Unesite naziv", Toast.LENGTH_SHORT).show(); return; }
                    allianceRepo.createAlliance(name, new AllianceRepository.Callback<String>() {
                        @Override public void onSuccess(String id) {
                            Toast.makeText(getContext(), "Savez kreiran", Toast.LENGTH_SHORT).show();
                            loadAllianceInfo();
                            new android.app.AlertDialog.Builder(getContext())
                                    .setMessage("Zelite li odmah da pozovete prijatelje u savez?")
                                    .setPositiveButton("Da", (dd, ww) -> inviteFriendsToAlliance())
                                    .setNegativeButton("Ne", null)
                                    .show();
                        }
                        @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton("Otkazi", null)
                .show();
    }

    private void inviteFriendsToAlliance() {
        allianceRepo.getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(AllianceRepository.AllianceInfo info) {
                if (info.allianceId == null || info.allianceId.isEmpty() || info.alliance == null) {
                    Toast.makeText(getContext(), "Niste u savezu", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!info.isLeader) {
                    Toast.makeText(getContext(), "Samo vodja moze slati pozive", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> friendIds = new ArrayList<>();
                for (UserPublic fp : friends) friendIds.add(fp.uid);
                allianceRepo.inviteFriends(info.allianceId, friendIds, new AllianceRepository.Callback<Void>() {
                    @Override public void onSuccess(Void v) { Toast.makeText(getContext(), "Pozivi poslati", Toast.LENGTH_SHORT).show(); }
                    @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
            }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void leaveAlliance() {
        allianceRepo.leaveAlliance(new AllianceRepository.Callback<Void>() {
            @Override public void onSuccess(Void data) {
                Toast.makeText(getContext(), "Napustili ste savez", Toast.LENGTH_SHORT).show();
                loadAllianceInfo();
            }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void disbandAlliance() {
        allianceRepo.getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(AllianceRepository.AllianceInfo info) {
                if (info.allianceId == null || info.allianceId.isEmpty() || info.alliance == null) {
                    Toast.makeText(getContext(), "Niste u savezu", Toast.LENGTH_SHORT).show();
                    return;
                }
                allianceRepo.destroyAlliance(info.allianceId, new AllianceRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) {
                        Toast.makeText(getContext(), "Savez ukinut", Toast.LENGTH_SHORT).show();
                        loadAllianceInfo();
                    }
                    @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
            }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void openAllianceDetails() {
        if (currentAllianceId == null || currentAllianceId.isEmpty()) return;
        new AllianceDetailsDialog(currentAllianceId).show(requireActivity().getSupportFragmentManager(), "AllianceDetails");
    }
}
