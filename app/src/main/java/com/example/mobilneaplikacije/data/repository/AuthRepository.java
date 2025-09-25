package com.example.mobilneaplikacije.data.repository;

import android.content.Context;

import com.example.mobilneaplikacije.data.model.Player;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private FirebaseAuth auth;
    private FirebaseFirestore db;


    public AuthRepository(){
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser currentUser() {return auth.getCurrentUser();}

    public void logOut() {auth.signOut();}

    public boolean isEmailVer() {
        FirebaseUser usr = this.currentUser();
        if(usr != null && usr.isEmailVerified())
            return true;
        return false;
    }

    public void register(String email, String pass, String username, String selAvatar, AuthCallback callback){

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    FirebaseUser usr = auth.getCurrentUser();
                    if(usr == null) return;

                    Player p = new Player(username, selAvatar, 1, "Pocetnik", 0, 0, 0, 0.0);
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

                    db.collection("users").document(usr.getUid()).set(data)
                            .addOnSuccessListener(v-> {
                                usr.sendEmailVerification()
                                        .addOnSuccessListener(ex->
                                            callback.onSucces("Uspesna registracija proveri email!"))
                                        .addOnFailureListener(ex-> callback.onFailure("Greska pri slanju emaila" + ex.getMessage()));
                            })
                            .addOnFailureListener(ex -> callback.onFailure("Greska pri cuvanju igraca" + ex.getMessage()));
                })
                .addOnFailureListener(ex -> callback.onFailure("Greska: " + ex.getMessage()));
    }


    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    FirebaseUser usr = this.currentUser();
                    if (usr != null && usr.isEmailVerified()) {
                        callback.onSucces("Uspensa prijava!");
                    } else {
                        auth.signOut();
                        callback.onFailure("Nalog nije verifikovan. Proveri email.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Greska: " + e.getMessage()));
    }

    public void changePassword(String currentPassword, String newPassword, AuthCallback callback) {
        FirebaseUser usr = this.currentUser();
        if (usr == null || usr.getEmail() == null) {
            callback.onFailure("Korisnik nije ulogovan.");
            return;
        }

        AuthCredential cred = EmailAuthProvider.getCredential(usr.getEmail(), currentPassword);
        usr.reauthenticate(cred)
                .addOnSuccessListener(v -> usr.updatePassword(newPassword)
                        .addOnSuccessListener(x -> callback.onSucces("Uspesno promenjea lozinka"))
                        .addOnFailureListener(e -> callback.onFailure("Greska pri promeni lozinke: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Pogresna trenutna lozinka."));
    }


    public interface AuthCallback {
        void onSucces(String mess);
        void onFailure(String mess);
    }

}
