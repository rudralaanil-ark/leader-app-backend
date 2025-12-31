package com.leaderapp.leader_backend.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
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

    private final Firestore db;
    private final FirebaseAuth auth;

    public FirebaseService() {
        this.db = FirestoreClient.getFirestore();
        this.auth = FirebaseAuth.getInstance();
    }

    private final Firestore firestore = FirestoreClient.getFirestore();

    // ✅ Verify Firebase ID Token
    public FirebaseToken verifyToken(String token) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().verifyIdToken(token);
    }

    // ✅ Create Monitor
    public UserRecord createMonitor(String email, String password, String fullName, String createdBy)
            throws FirebaseAuthException, ExecutionException, InterruptedException {

        // 1️⃣ Create user in Firebase Auth
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(fullName)
                .setEmailVerified(true)
                .setDisabled(false);

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

        // 2️⃣ Save in Firestore
        Map<String, Object> monitorData = new HashMap<>();
        monitorData.put("uid", userRecord.getUid());
        monitorData.put("email", email);
        monitorData.put("fullName", fullName);
        monitorData.put("role", "monitor");
        monitorData.put("status", true);
        monitorData.put("createdBy", createdBy);
        monitorData.put("createdAt", System.currentTimeMillis());

        firestore.collection("users").document(userRecord.getUid()).set(monitorData);

        return userRecord;
    }

    // ✅ Update Email — Fix: Also updates Firestore
    public void updateEmail(String uid, String newEmail) throws FirebaseAuthException {
        // 1️⃣ Update Firebase Auth
        FirebaseAuth.getInstance()
                .updateUser(new UserRecord.UpdateRequest(uid).setEmail(newEmail));

        // 2️⃣ Also update Firestore document
        try {
            FirestoreClient.getFirestore()
                    .collection("users")
                    .document(uid)
                    .update("email", newEmail);

            System.out.println("✅ Firestore email updated for user: " + uid);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update Firestore email for " + uid + ": " + e.getMessage());
        }
    }

    // ✅ Update Password — Adds optional audit log
    public void updatePassword(String uid, String newPassword) throws FirebaseAuthException {
        // 1️⃣ Update Firebase Auth password
        FirebaseAuth.getInstance()
                .updateUser(new UserRecord.UpdateRequest(uid).setPassword(newPassword));

        // 2️⃣ Optional audit: track last password change
        try {
            firestore.collection("users")
                    .document(uid)
                    .update("lastPasswordChange", System.currentTimeMillis());
            System.out.println("🔐 Password updated for " + uid);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log password update for " + uid + ": " + e.getMessage());
        }
    }

    // ✅ Set Active/Inactive Status
    public void setMonitorStatus(String uid, boolean active)
            throws FirebaseAuthException, ExecutionException, InterruptedException {

        FirebaseAuth.getInstance()
                .updateUser(new UserRecord.UpdateRequest(uid).setDisabled(!active));

        firestore.collection("users")
                .document(uid)
                .update("status", active);
    }

    // ✅ Fetch all monitors created by a specific admin
    public List<Map<String, Object>> fetchMonitorsByAdmin(String adminUid)
            throws ExecutionException, InterruptedException {

        List<Map<String, Object>> monitorsList = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection("users")
                .whereEqualTo("role", "monitor")
                .whereEqualTo("createdBy", adminUid)
                .get();

        List<QueryDocumentSnapshot> docs = query.get().getDocuments();
        for (QueryDocumentSnapshot doc : docs) {
            monitorsList.add(doc.getData());
        }

        return monitorsList;
    }

    // ✅ Delete Monitor (Auth + Firestore)
    public void deleteMonitor(String uid) throws Exception {
        // Delete from Firebase Authentication
        auth.deleteUser(uid);

        // Delete from Firestore users collection
        db.collection("users").document(uid).delete();

        System.out.println("🗑️ Monitor deleted successfully: " + uid);
    }
}
