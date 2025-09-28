package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.UserPublic;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class FriendsRepository {
    public interface Callback<T> { void onSuccess(@Nullable T data); void onError(Exception e); }

    private final FirebaseFirestore db;
    private final String uid;

    public FriendsRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser usr = FirebaseAuth.getInstance().getCurrentUser();
        if (usr == null) throw new IllegalStateException("User not logged in");
        this.uid = usr.getUid();
    }

    private DocumentReference userDoc(String u) { return db.collection("users").document(u); }
    private CollectionReference friendsCol(String u) { return userDoc(u).collection("friends"); }
    private CollectionReference requestsCol(String u) { return userDoc(u).collection("friendRequests"); }

    public void searchUsersByUsername(String query, Callback<List<UserPublic>> cb) {
        db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener(snap -> {
                List<UserPublic> list = new ArrayList<>();
                for (DocumentSnapshot d : snap.getDocuments()) {
                    String u = d.getId();
                    if (u.equals(uid)) continue;
                    UserPublic up = new UserPublic(u, d.getString("username"), d.getString("avatar"));
                    up.level = d.getLong("level") == null ? null : d.getLong("level").intValue();
                    up.title = d.getString("title");
                    up.pp = d.getLong("pp") == null ? null : d.getLong("pp").intValue();
                    up.coins = d.getLong("coins") == null ? null : d.getLong("coins").intValue();
                    list.add(up);
                }
                addFriendStatus(list, cb);
            })
            .addOnFailureListener(cb::onError);
    }

    private void addFriendStatus(List<UserPublic> list, Callback<List<UserPublic>> cb) {
        friendsCol(uid).get().addOnSuccessListener(friendsSnap -> {
            Set<String> friends = new HashSet<>();
            for (DocumentSnapshot d : friendsSnap)
                friends.add(d.getId());

            requestsCol(uid).get().addOnSuccessListener(reqSnap -> {
                Set<String> incoming = new HashSet<>();
                for (DocumentSnapshot d : reqSnap) {
                    String from = d.getString("from");
                    String status = d.getString("status");
                    if ("pending".equals(status) && from != null)
                        incoming.add(from);
                }

                requestsCol(uid).whereEqualTo("from", uid).get().addOnSuccessListener(outSnap -> {
                    for (UserPublic u : list) {
                        if (friends.contains(u.uid)) u.status = "friend";
                        else if (incoming.contains(u.uid)) u.status = "incoming";
                        else u.status = "none";
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void listFriends(Callback<List<UserPublic>> cb) {
        friendsCol(uid).get().addOnSuccessListener(snap -> {
            List<UserPublic> friends = new ArrayList<>();
            List<String> freindsIds = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments())
                freindsIds.add(d.getId());
            if (freindsIds.isEmpty()) { cb.onSuccess(friends); return; }
            db.collection("users").whereIn(FieldPath.documentId(), freindsIds).get()
                .addOnSuccessListener(users -> {
                    for (DocumentSnapshot u : users) {
                        UserPublic up = new UserPublic(u.getId(), u.getString("username"), u.getString("avatar"));
                        up.level = u.getLong("level") == null ? null : u.getLong("level").intValue();
                        up.title = u.getString("title");
                        up.pp = u.getLong("pp") == null ? null : u.getLong("pp").intValue();
                        up.coins = u.getLong("coins") == null ? null : u.getLong("coins").intValue();
                        up.status = "friend";
                        friends.add(up);
                    }
                    cb.onSuccess(friends);
                })
                .addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void sendFriendRequest(String otherUid, Callback<Void> cb) {
        if (otherUid.equals(uid)) { cb.onError(new IllegalArgumentException("CANNOT_ADD_SELF")); return; }
        friendsCol(uid).document(otherUid).get().addOnSuccessListener(friendDoc -> {
            if (friendDoc.exists()) { cb.onError(new IllegalStateException("ALREADY_FRIENDS")); return; }

            requestsCol(otherUid).whereEqualTo("from", uid).whereEqualTo("status", "pending").limit(1).get()
                .addOnSuccessListener(pendSnap -> {
                    if (!pendSnap.isEmpty()) { cb.onError(new IllegalStateException("ALREADY_REQUESTED")); return; }
                    Map<String, Object> req = new HashMap<>();
                    req.put("from", uid);
                    req.put("status", "pending");
                    req.put("createdAt", FieldValue.serverTimestamp());
                    requestsCol(otherUid).add(req)
                        .addOnSuccessListener(r -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void listIncomingRequests(Callback<List<UserPublic>> cb) {
        requestsCol(uid).whereEqualTo("status", "pending").get().addOnSuccessListener(snap -> {
            List<String> senders = new ArrayList<>();
            for (DocumentSnapshot d : snap) {
                String from = d.getString("from");
                if (from != null) senders.add(from);
            }
            if (senders.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
            db.collection("users").whereIn(FieldPath.documentId(), senders).get().addOnSuccessListener(users -> {
                List<UserPublic> usersTorequest = new ArrayList<>();
                for (DocumentSnapshot u : users) {
                    UserPublic up = new UserPublic(u.getId(), u.getString("username"), u.getString("avatar"));
                    up.level = u.getLong("level") == null ? null : u.getLong("level").intValue();
                    up.title = u.getString("title");
                    up.pp = u.getLong("pp") == null ? null : u.getLong("pp").intValue();
                    up.coins = u.getLong("coins") == null ? null : u.getLong("coins").intValue();
                    up.status = "incoming";
                    usersTorequest.add(up);
                }
                cb.onSuccess(usersTorequest);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void acceptRequest(String fromUid, Callback<Void> cb) {
        requestsCol(uid).whereEqualTo("from", fromUid).whereEqualTo("status", "pending").get()
            .addOnSuccessListener(snap -> {
                if (snap.isEmpty()) { cb.onError(new IllegalStateException("NO_PENDING")); return; }
                WriteBatch batch = db.batch();
                for (DocumentSnapshot d : snap) batch.update(d.getReference(), "status", "accepted");
                batch.set(friendsCol(uid).document(fromUid), new HashMap<>());
                batch.set(friendsCol(fromUid).document(uid), new HashMap<>());
                batch.commit().addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
            })
            .addOnFailureListener(cb::onError);
    }

}
