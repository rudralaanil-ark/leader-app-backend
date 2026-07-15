package com.leaderapp.leader_backend.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    private Firestore firestore;
    private FirebaseAuth auth;

    private boolean isFirebaseAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    private Firestore getFirestore() {
        if (!isFirebaseAvailable()) {
            throw new IllegalStateException("Firebase is not initialized");
        }
        if (firestore == null) {
            firestore = FirestoreClient.getFirestore();
        }
        return firestore;
    }

    private FirebaseAuth getAuth() {
        if (!isFirebaseAvailable()) {
            throw new IllegalStateException("Firebase is not initialized");
        }
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    public FirebaseToken verifyToken(String token) throws FirebaseAuthException {
        return getAuth().verifyIdToken(token);
    }

    public UserRecord createMonitor(String email, String password, String fullName, String createdBy)
            throws FirebaseAuthException {

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(fullName)
                .setEmailVerified(true)
                .setDisabled(false);

        UserRecord userRecord = getAuth().createUser(request);

        Map<String, Object> monitorData = new HashMap<>();
        monitorData.put("uid", userRecord.getUid());
        monitorData.put("email", email);
        monitorData.put("fullName", fullName);
        monitorData.put("role", "monitor");
        monitorData.put("status", true);
        monitorData.put("createdBy", createdBy);
        monitorData.put("createdAt", System.currentTimeMillis());

        getFirestore()
                .collection("users")
                .document(userRecord.getUid())
                .set(monitorData);

        return userRecord;
    }

    public void updateEmail(String uid, String newEmail) throws FirebaseAuthException {
        getAuth().updateUser(new UserRecord.UpdateRequest(uid).setEmail(newEmail));
        getFirestore().collection("users").document(uid).update("email", newEmail);
    }

    public void updatePassword(String uid, String newPassword) throws FirebaseAuthException {
        getAuth().updateUser(new UserRecord.UpdateRequest(uid).setPassword(newPassword));
        getFirestore().collection("users")
                .document(uid)
                .update("lastPasswordChange", System.currentTimeMillis());
    }

    public void setMonitorStatus(String uid, boolean active) throws FirebaseAuthException {
        getAuth().updateUser(new UserRecord.UpdateRequest(uid).setDisabled(!active));
        getFirestore().collection("users").document(uid).update("status", active);
    }

    public List<Map<String, Object>> fetchMonitorsByAdmin(String adminUid) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = getFirestore()
                .collection("users")
                .whereEqualTo("role", "monitor")
                .whereEqualTo("createdBy", adminUid)
                .get();

        for (QueryDocumentSnapshot doc : query.get().getDocuments()) {
            list.add(doc.getData());
        }
        return list;
    }

    public void deleteMonitor(String uid) throws FirebaseAuthException {
        getAuth().deleteUser(uid);
        getFirestore().collection("users").document(uid).delete();
    }
}

