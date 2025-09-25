package com.example.mobilneaplikacije.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class RegisterFragment extends Fragment {

    private EditText etEmail, etPassword, etPassword2, etUsername;
    private Button btnRegister;
    private String selectedAvatar = null;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register, container, false);

        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        etPassword2 = v.findViewById(R.id.etPassword2);
        etUsername = v.findViewById(R.id.etUsername);
        btnRegister = v.findViewById(R.id.btnRegister);

        RecyclerView recyclerAvatars = v.findViewById(R.id.recyclerAvatars);
        recyclerAvatars.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        AvatarAdapter adapter = new AvatarAdapter(a -> selectedAvatar = a);
        recyclerAvatars.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(x -> doRegister());

        return v;
    }

    private void doRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String password2 = etPassword2.getText().toString();
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(password2) || TextUtils.isEmpty(username) || selectedAvatar == null) {
            toast("Moras popuniti sva polja i izabrati nekog avatara");
            return;
        }
        if (!password.equals(password2)) {
            toast("Lozinke se ne poklapaju");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    FirebaseUser u = auth.getCurrentUser();
                    if (u == null) return;

                    Player p = new Player(username, selectedAvatar, 1, "Pocetnik", 0, 0, 0, 0.0);
                    Map<String, Object> data = new HashMap<>();
                    data.put("username", p.getUsername());
                    data.put("avatar", p.getAvatar());
                    data.put("level", p.getLevel());
                    data.put("title", p.getTitle());
                    data.put("pp", p.getPp());
                    data.put("xp", p.getXp());
                    data.put("coins", p.getCoins());
                    data.put("successRate", p.getSuccessRate());
                    data.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users").document(u.getUid()).set(data)
                            .addOnSuccessListener(v -> {
                                u.sendEmailVerification()
                                        .addOnSuccessListener(e -> {
                                            toast("Registracija uspesna! Proveri email za verifikaciju.");
                                            auth.signOut();

                                            // prosledi ga na login fragment
                                            requireActivity().getSupportFragmentManager()
                                                    .beginTransaction()
                                                    .replace(R.id.fragment_container, new LoginFragment())
                                                    .commit();
                                        });
                            });
                })
                .addOnFailureListener(e -> toast("Greska: " + e.getMessage()));
    }

    private void toast(String m) {
        Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
