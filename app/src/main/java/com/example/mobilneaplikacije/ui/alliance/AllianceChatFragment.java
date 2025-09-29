package com.example.mobilneaplikacije.ui.alliance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.MainActivity;
import com.example.mobilneaplikacije.data.model.AllianceMessage;
import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AllianceChatFragment extends Fragment {
    private static final String ARG_ALLIANCE_ID = "alliance_id";
    public static AllianceChatFragment newInstance(String allianceId) {
        Bundle b = new Bundle();
        b.putString(ARG_ALLIANCE_ID, allianceId);
        AllianceChatFragment f = new AllianceChatFragment(); f.setArguments(b); return f;
    }

    private String allianceId;
    private RecyclerView rv;
    private AllianceChatMessagesAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private AllianceRepository repo;
    private String activeUsrId;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alliance_chat, container, false);
        allianceId = getArguments() != null ? getArguments().getString(ARG_ALLIANCE_ID) : null;

        rv = v.findViewById(R.id.rvMessages);
        etMessage = v.findViewById(R.id.etMessage);
        btnSend = v.findViewById(R.id.btnSend);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new AllianceChatMessagesAdapter(new ArrayList<>());
        rv.setAdapter(adapter);
        repo = new AllianceRepository();
        activeUsrId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter.setMyUid(activeUsrId);

        View.OnClickListener sender = view1 -> {
            String text = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text) || allianceId == null) return;
            repo.sendMessage(allianceId, text, new AllianceRepository.Callback<Void>() {
                @Override public void onSuccess(Void data) {
                    etMessage.setText("");
                    loadMessages();
                }
                @Override public void onError(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
            });
        };
        btnSend.setOnClickListener(sender);
        etMessage.setOnEditorActionListener((v1, actionId, event) -> {
            sender.onClick(v1);
            return true;
        });

        return v;
    }

    @Override public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(false);
        }
        loadMessages();
    }

    @Override public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(true);
        }
    }

    private void loadMessages() {
        if (allianceId == null) return;
        repo.listMessages(allianceId, new AllianceRepository.Callback<List<AllianceMessage>>() {
            @Override public void onSuccess(List<AllianceMessage> data) {
                adapter.setItems(data);
                View noMessagesView = getView() != null ? getView().findViewById(R.id.tvEmptyChat) : null;
                if (noMessagesView != null) noMessagesView.setVisibility((data == null || data.isEmpty()) ? View.VISIBLE : View.GONE);
                rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
            }
            @Override public void onError(Exception e) { }
        });
    }
}
