package com.example.mobilneaplikacije.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.MainActivity;
import com.example.mobilneaplikacije.R;
import com.google.firebase.auth.*;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        Button loginBtn = v.findViewById(R.id.btnLogin);
        Button goRegisterBtn = v.findViewById(R.id.btnGoRegister);

        auth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(x -> LogIn());

        // prebaci ga na register
        goRegisterBtn.setOnClickListener(x ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RegisterFragment())
                        .addToBackStack(null)
                        .commit());
        return v;
    }

    private void LogIn() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            toast("Popuni oba polja!!!");
            return;
        }

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    FirebaseUser u = auth.getCurrentUser();
                    if (u != null && u.isEmailVerified()) {
                        toast("Dobrodosao!");

                        // prikazi navbarove
                        ((MainActivity) requireActivity()).setBottomNavVisible(true);
                        ((MainActivity) requireActivity()).setToolbarVisible(true);

                        // prebaci na TaskListFragment i ocisti backstack
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container,
                                        new com.example.mobilneaplikacije.ui.task.TaskListFragment())
                                .commit();
                    } else {
                        toast("Nalog nije aktiviran. Proveri email.");
                        auth.signOut();
                    }
                })
                .addOnFailureListener(e -> toast("Greska: " + e.getMessage()));
    }

    private void toast(String m) {
        Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
