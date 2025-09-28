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

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    private FriendsRepository repo;
    private EditText etSearch;
    private androidx.recyclerview.widget.RecyclerView rv;
    private FriendsAdapter adapter;
    private final List<UserPublic> friends = new ArrayList<>();
    private final List<UserPublic> results = new ArrayList<>();
    private final List<UserPublic> incoming = new ArrayList<>();

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

        etSearch = v.findViewById(R.id.etSearch);
        rv = v.findViewById(R.id.rvFriends);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        adapter = new FriendsAdapter(new FriendsAdapter.ActionHandler() {
            @Override public void onAccept(UserPublic u) { onIncomingAction(u); }
            @Override public void onAdd(UserPublic u) { onSearchAction(u); }
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnSearch).setOnClickListener(view -> doSearch());
        v.findViewById(R.id.btnShowMyQr).setOnClickListener(view -> showMyQr());
        v.findViewById(R.id.btnScanQr).setOnClickListener(view -> checkCameraThenScan());

        return v;
    }

    @Override public void onResume() { super.onResume(); loadFriends(); loadIncoming(); }

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
}
