package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Alliance;
import com.example.mobilneaplikacije.data.model.AllianceInvite;
import com.example.mobilneaplikacije.data.model.UserPublic;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.*;
import com.google.firebase.firestore.Query;
import com.example.mobilneaplikacije.data.model.AllianceMessage;

public class AllianceRepository {
    public interface Callback<T> { void onSuccess(@Nullable T data); void onError(Exception e); }
    public interface Listener { void onChange(); void onError(Exception e); }
    public interface InviteAddedCallback { void onInviteAdded(AllianceInvite invite, @Nullable String allianceName, @Nullable String inviterUsername); void onError(Exception e); }
    public static class AllianceInfo {
        public String allianceId;
        public Alliance alliance; 
        public boolean isLeader;
        public AllianceInfo(String allianceId, Alliance alliance, boolean isLeader) {
            this.allianceId = allianceId;
            this.alliance = alliance;
            this.isLeader = isLeader;
        }
    }
    private final FirebaseFirestore db;
    private final String uid;
    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser usr = FirebaseAuth.getInstance().getCurrentUser();
        if (usr == null) throw new IllegalStateException("User not logged in");
        this.uid = usr.getUid();
    }

    private CollectionReference allianceCol() { return db.collection("alliances"); }
    private DocumentReference userDoc(String uid) { return db.collection("users").document(uid); }
    private com.google.firebase.firestore.CollectionReference messagesCol(String allianceId) { return allianceCol().document(allianceId).collection("messages"); }

    public void createAlliance(String name, Callback<String> cb) {
        userDoc(uid).get().addOnSuccessListener(userSnap -> {
            String currentAlliance = userSnap.getString("allianceId");
            if (currentAlliance != null && !currentAlliance.isEmpty()) {
                cb.onError(new IllegalStateException("ALREADY_IN_ALLIANCE")); return; }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("leaderUid", uid);
            data.put("isSpecialMissionActive", false);
            data.put("createdAt", FieldValue.serverTimestamp());

            allianceCol().add(data).addOnSuccessListener(doc -> {
                userDoc(uid).update("allianceId", doc.getId())
                    .addOnSuccessListener(v -> cb.onSuccess(doc.getId()))
                    .addOnFailureListener(cb::onError);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }
    
    
    public void destroyAlliance(String allianceId, Callback<Void> cb) {
        allianceCol().document(allianceId).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { cb.onError(new IllegalStateException("NOT_FOUND")); return; }
            String leaderUid = snap.getString("leaderUid");
            Boolean isMissionActive = snap.getBoolean("isSpecialMissionActive");
            if (!uid.equals(leaderUid)) { cb.onError(new IllegalStateException("NOT_LEADER")); return; }
            if (isMissionActive) { cb.onError(new IllegalStateException("MISSION_ACTIVE")); return; }

            db.collection("users").whereEqualTo("allianceId", allianceId).get().addOnSuccessListener(users -> {
                WriteBatch b = db.batch();
                for (DocumentSnapshot u : users)
                    b.update(u.getReference(), "allianceId", FieldValue.delete());
                b.delete(allianceCol().document(allianceId));
                b.commit().addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void leaveAlliance(Callback<Void> cb) {
        userDoc(uid).get().addOnSuccessListener(userSnap -> {
            String allianceId = userSnap.getString("allianceId");
            if (allianceId == null || allianceId.isEmpty()) { cb.onError(new IllegalStateException("NO_ALLIANCE")); return; }

            allianceCol().document(allianceId).get().addOnSuccessListener(a -> {
                if (!a.exists()) {
                    userDoc(uid).update("allianceId", FieldValue.delete())
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                    return;
                }
                Boolean isMissionActive = a.getBoolean("isSpecialMissionActive");
                if (isMissionActive) { cb.onError(new IllegalStateException("MISSION_ACTIVE")); return; }

                String leader = a.getString("leaderUid");
                if (uid.equals(leader)) { cb.onError(new IllegalStateException("LEADER_CANNOT_LEAVE")); return; }
                userDoc(uid).update("allianceId", FieldValue.delete())
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void inviteFriends(String allianceId, List<String> friendUids, Callback<Void> cb) {
        allianceCol().document(allianceId).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { cb.onError(new IllegalStateException("NOT_FOUND")); return; }
            String leader = snap.getString("leaderUid");
            if (!uid.equals(leader)) { cb.onError(new IllegalStateException("NOT_LEADER")); return; }

            WriteBatch batch = db.batch();
            for (String friendUid : friendUids) {
                Map<String, Object> inv = new HashMap<>();
                inv.put("allianceId", allianceId);
                inv.put("from", uid);
                inv.put("status", "pending");
                inv.put("createdAt", FieldValue.serverTimestamp());
                batch.set(userDoc(friendUid).collection("allianceInvites").document(allianceId), inv, SetOptions.merge());
            }
            batch.commit().addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void respondToInvite(String allianceId, boolean accept, Callback<Void> cb) {
        DocumentReference allianceInviteRef =
                userDoc(uid).collection("allianceInvites").document(allianceId);

        allianceInviteRef.get().addOnSuccessListener(inv -> {
            if (!inv.exists()) {
                cb.onError(new IllegalStateException("INVITE_NOT_FOUND"));
                return;
            }

            if (!accept) {
                allianceInviteRef.update("status", "declined")
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
                return;
            }

            allianceCol().document(allianceId).get().addOnSuccessListener(a -> {
                if (!a.exists()) {
                    cb.onError(new IllegalStateException("ALLIANCE_NOT_FOUND"));
                    return;
                }
                userDoc(uid).get().addOnSuccessListener(userSnap -> {
                    String currentAlliance = userSnap.getString("allianceId");
                    if (allianceId.equals(currentAlliance)) {
                        allianceInviteRef.update("status", "accepted")
                                .addOnSuccessListener(v -> cb.onSuccess(null))
                                .addOnFailureListener(cb::onError);
                        return;
                    }
                    if (currentAlliance == null || currentAlliance.isEmpty()) {
                        WriteBatch b = db.batch();
                        b.update(userDoc(uid), "allianceId", allianceId);
                        b.update(allianceInviteRef, "status", "accepted");
                        b.commit()
                                .addOnSuccessListener(v -> cb.onSuccess(null))
                                .addOnFailureListener(cb::onError);
                        return;
                    }
                    leaveAlliance(new Callback<Void>() {
                        @Override public void onSuccess(@Nullable Void ignore) {
                            WriteBatch b = db.batch();
                            b.update(userDoc(uid), "allianceId", allianceId);
                            b.update(allianceInviteRef, "status", "accepted");
                            b.commit()
                                    .addOnSuccessListener(v -> cb.onSuccess(null))
                                    .addOnFailureListener(cb::onError);
                        }
                        @Override public void onError(Exception e) {
                            cb.onError(e);
                        }
                    });

                }).addOnFailureListener(cb::onError);

            }).addOnFailureListener(cb::onError);

        }).addOnFailureListener(cb::onError);
    }


    public void getAlliance(String allianceId, Callback<Alliance> cb) {
        allianceCol().document(allianceId).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { cb.onError(new IllegalStateException("NOT_FOUND")); return; }
            Alliance a = new Alliance(snap.getId(), snap.getString("name"), snap.getString("leaderUid"), snap.getBoolean("isSpecialMissionActive").toString());
            cb.onSuccess(a);
        }).addOnFailureListener(cb::onError);
    }

    public void getMembers(String allianceId, Callback<List<UserPublic>> cb) {
        db.collection("users").whereEqualTo("allianceId", allianceId).get().addOnSuccessListener(qs -> {
            List<UserPublic> users = new ArrayList<>();
            for (DocumentSnapshot d : qs) {
                String id = d.getId();
                String username = d.getString("username");
                String avatar = d.getString("avatar");
                users.add(new UserPublic(id, username, avatar));
            }
            cb.onSuccess(users);
        }).addOnFailureListener(cb::onError);
    }

    public void listIncomingInvites(Callback<List<AllianceInvite>> cb) {
        userDoc(uid).collection("allianceInvites").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(qs -> {
                    List<AllianceInvite> list = new ArrayList<>();
                    List<String> allianceIds = new ArrayList<>();
                    for (DocumentSnapshot d : qs) {
                        AllianceInvite inv = new AllianceInvite(
                                d.getString("allianceId"), d.getString("from"), d.getString("status")
                        );
                        list.add(inv);
                        if (inv.allianceId != null)
                            allianceIds.add(inv.allianceId);
                    }
                    if (allianceIds.isEmpty()) { cb.onSuccess(list); return; }
                    allianceCol().whereIn(FieldPath.documentId(), allianceIds).get().addOnSuccessListener(as -> {
                        Map<String, String> names = new HashMap<>();
                        for (DocumentSnapshot a : as)
                            names.put(a.getId(), a.getString("name"));
                        for (AllianceInvite inv : list) {
                            if (inv.allianceId != null && names.containsKey(inv.allianceId))
                                inv.allianceName = names.get(inv.allianceId);
                        }
                        cb.onSuccess(list);
                    }).addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }
    public ListenerRegistration listenIncomingInvites(InviteAddedCallback cb) {
        return userDoc(uid).collection("allianceInvites")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((QuerySnapshot snap, FirebaseFirestoreException err) -> {
                    if (err != null || snap == null) { if (err != null) cb.onError(err); return; }
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;
                        DocumentSnapshot d = dc.getDocument();
                        AllianceInvite inv = new AllianceInvite(d.getString("allianceId"), d.getString("from"), d.getString("status"));
                        final String allianceId = inv.allianceId;
                        final String fromUid = inv.from;
                        Task<DocumentSnapshot> aTask = (allianceId != null)
                                ? allianceCol().document(allianceId).get()
                                : Tasks.forResult(null);
                        Task<DocumentSnapshot> uTask = (fromUid != null)
                                ? userDoc(fromUid).get()
                                : Tasks.forResult(null);
                        Tasks.whenAllSuccess(aTask, uTask).addOnSuccessListener(list -> {
                            String allianceName = null;
                            String inviterName = null;
                            try {
                                if (aTask.getResult() != null && aTask.getResult().exists())
                                    allianceName = aTask.getResult().getString("name");
                                if (uTask.getResult() != null && uTask.getResult().exists())
                                    inviterName = uTask.getResult().getString("username");
                            } catch (Exception ignored) {}
                            cb.onInviteAdded(inv, allianceName, inviterName);
                        }).addOnFailureListener(cb::onError);
                    }
                });
    }

    public void getMyAllianceInfo(Callback<AllianceInfo> cb) {
        userDoc(uid).get().addOnSuccessListener(userSnap -> {
            String allianceId = userSnap.getString("allianceId");
            if (allianceId == null || allianceId.isEmpty()) {
                cb.onSuccess(new AllianceInfo(null, null, false));
                return;
            }
            allianceCol().document(allianceId).get().addOnSuccessListener(a -> {
                if (!a.exists()) {
                    cb.onSuccess(new AllianceInfo(allianceId, null, false));
                    return;
                }
                Alliance al = new Alliance(a.getId(), a.getString("name"), a.getString("leaderUid"), a.getBoolean("isSpecialMissionActive").toString());
                boolean isLeader = uid.equals(al.leaderUid);
                cb.onSuccess(new AllianceInfo(allianceId, al, isLeader));
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void listMessages(String allianceId, Callback<List<AllianceMessage>> cb) {
        messagesCol(allianceId).orderBy("createdAt", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(qs -> {
                    List<AllianceMessage> list = new ArrayList<>();
                    for (DocumentSnapshot d : qs) {
                        list.add(new AllianceMessage(
                                d.getId(),
                                allianceId,
                                d.getString("senderUid"),
                                d.getString("senderUsername"),
                                d.getString("text"),
                                d.getDate("createdAt")
                        ));
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void sendMessage(String allianceId, String text, Callback<Void> cb) {
        if (text == null || text.trim().isEmpty()) { cb.onError(new IllegalArgumentException("EMPTY")); return; }
        userDoc(uid).get().addOnSuccessListener(u -> {
            String username = u.getString("username");
            Map<String, Object> data = new HashMap<>();
            data.put("senderUid", uid);
            data.put("senderUsername", username);
            data.put("text", text.trim());
            data.put("createdAt", FieldValue.serverTimestamp());
            messagesCol(allianceId).add(data)
                    .addOnSuccessListener(doc -> cb.onSuccess(null))
                    .addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }
}
