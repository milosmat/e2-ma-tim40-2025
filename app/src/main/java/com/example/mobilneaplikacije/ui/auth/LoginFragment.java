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
import com.example.mobilneaplikacije.data.repository.AuthRepository;
import com.google.firebase.auth.*;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private FirebaseAuth auth;
    private AuthRepository authRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        Button loginBtn = v.findViewById(R.id.btnLogin);
        Button goRegisterBtn = v.findViewById(R.id.btnGoRegister);

        auth = FirebaseAuth.getInstance();
        authRepo = new AuthRepository();

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

        authRepo.login(email, pass, new AuthRepository.AuthCallback() {
            @Override
            public void onSucces(String message) {
                toast(message);
                ((MainActivity) requireActivity()).setBottomNavVisible(true);
                ((MainActivity) requireActivity()).setToolbarVisible(true);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
            }
            @Override
            public void onFailure(String message) {
                toast(message);
            }
        });
    }

    private void toast(String m) {
        Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
