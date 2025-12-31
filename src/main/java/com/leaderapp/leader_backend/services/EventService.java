package com.leaderapp.leader_backend.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class EventService {

    private final Firestore db;

    public EventService() {
        this.db = FirestoreClient.getFirestore();
    }

    // ✅ Add Event
    public void addEvent(Map<String, Object> eventData) throws ExecutionException, InterruptedException {
        db.collection("events").add(eventData).get();
    }

    // ✅ Get all events
    public List<Map<String, Object>> getAllEvents() throws ExecutionException, InterruptedException {
        var docs = db.collection("events")
                .orderBy("eventDate", Query.Direction.ASCENDING)
                .get().get().getDocuments();

        List<Map<String, Object>> events = new ArrayList<>();
        for (var doc : docs) {
            Map<String, Object> data = doc.getData();
            data.put("id", doc.getId());

            // Add RSVP count
            var rsvpSnaps = db.collection("events").document(doc.getId())
                    .collection("rsvps").get().get().getDocuments();
            data.put("rsvpCount", rsvpSnaps.size());

            events.add(data);
        }
        return events;
    }

    // ✅ Update Event
    public boolean updateEvent(String id, Map<String, Object> updatedData)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("events").document(id);
        DocumentSnapshot docSnap = docRef.get().get();
        if (docSnap.exists()) {
            updatedData.put("updatedAt", new Date().getTime());
            ApiFuture<WriteResult> future = docRef.update(updatedData);
            future.get();
            return true;
        }
        return false;
    }

    // ✅ Delete Event
    public boolean deleteEvent(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("events").document(id);
        DocumentSnapshot docSnap = docRef.get().get();
        if (docSnap.exists()) {
            docRef.delete().get();
            return true;
        }
        return false;
    }

    // ✅ RSVP Methods (Firestore Subcollection)
    public boolean addRsvp(String eventId, String uid) throws ExecutionException, InterruptedException {
        DocumentReference rsvpRef = db.collection("events").document(eventId).collection("rsvps").document(uid);
        DocumentSnapshot snap = rsvpRef.get().get();
        if (snap.exists()) return false; // already RSVPed

        Map<String, Object> data = new HashMap<>();
        data.put("joinedAt", new Date().getTime());
        rsvpRef.set(data).get();
        return true;
    }

    public boolean removeRsvp(String eventId, String uid) throws ExecutionException, InterruptedException {
        DocumentReference rsvpRef = db.collection("events").document(eventId).collection("rsvps").document(uid);
        DocumentSnapshot snap = rsvpRef.get().get();
        if (!snap.exists()) return false;
        rsvpRef.delete().get();
        return true;
    }

    public int getRsvpCount(String eventId) throws ExecutionException, InterruptedException {
        var snaps = db.collection("events").document(eventId).collection("rsvps").get().get().getDocuments();
        return snaps.size();
    }

    // ✅ FIXED — Get Attendee Details
    public List<Map<String, Object>> getRsvpUserDetails(String eventId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // 🔹 Get all RSVP subcollection documents
        CollectionReference rsvpsRef = db.collection("events").document(eventId).collection("rsvps");
        List<QueryDocumentSnapshot> rsvpDocs = rsvpsRef.get().get().getDocuments();

        if (rsvpDocs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> userDetails = new ArrayList<>();

        for (var rsvpDoc : rsvpDocs) {
            String uid = rsvpDoc.getId();
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", uid);
            userData.put("displayName", "Unknown");
            userData.put("email", "N/A");
            userData.put("phoneNumber", "-");

            try {
                // ✅ First: Fetch from Firestore "users" collection (your app’s profile data)
                DocumentSnapshot userDoc = db.collection("users").document(uid).get().get();
                if (userDoc.exists()) {
                    if (userDoc.contains("name")) userData.put("displayName", userDoc.getString("name"));
                    if (userDoc.contains("email")) userData.put("email", userDoc.getString("email"));
                    if (userDoc.contains("phone")) userData.put("phoneNumber", userDoc.getString("phone"));
                }

                // ✅ Second: Fallback to Firebase Auth info if Firestore has none
                if ("Unknown".equals(userData.get("displayName")) || "N/A".equals(userData.get("email"))) {
                    var userRecord = com.google.firebase.auth.FirebaseAuth.getInstance().getUser(uid);
                    if (userRecord != null) {
                        if (userRecord.getDisplayName() != null && !userRecord.getDisplayName().isEmpty()) {
                            userData.put("displayName", userRecord.getDisplayName());
                        }
                        if (userRecord.getEmail() != null) {
                            userData.put("email", userRecord.getEmail());
                        }
                        if (userRecord.getPhoneNumber() != null) {
                            userData.put("phoneNumber", userRecord.getPhoneNumber());
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("⚠️ Could not fetch user info for UID: " + uid + " - " + e.getMessage());
            }

            userDetails.add(userData);
        }

        return userDetails;
    }


    // ✅ Check RSVP status for a specific user
    public boolean isUserRsvped(String eventId, String uid)
            throws ExecutionException, InterruptedException {
        var snap = db.collection("events").document(eventId)
                .collection("rsvps").document(uid).get().get();
        return snap.exists();
    }
}
