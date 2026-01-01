package com.leaderapp.leader_backend.services;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NewsNotificationService {

    private final Firestore firestore;
    private final FirebaseMessaging firebaseMessaging;

    public NewsNotificationService() {
        this.firestore = FirestoreClient.getFirestore();
        this.firebaseMessaging = FirebaseMessaging.getInstance();
        startListener();
    }

    private void startListener() {
        firestore.collection("news")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            handleNewNews(dc.getDocument());
                        }
                    }
                });
    }

    private void handleNewNews(DocumentSnapshot doc) {
        try {
            Timestamp createdAt = doc.getTimestamp("createdAt");
            if (createdAt == null) return;

            // ⛔ Ignore old news (important on server restart)
            long diff = System.currentTimeMillis() - createdAt.toDate().getTime();
            if (diff > 60_000) return;

            String title = doc.getString("title");
            String newsId = doc.getId();

            sendNotification(title, newsId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String title, String newsId) {
        try {
            Message message = Message.builder()
                    .setTopic("all_users")
                    .setNotification(
                            Notification.builder()
                                    .setTitle("📰 New News")
                                    .setBody(title)
                                    .build()
                    )
                    .putData("type", "news")
                    .putData("id", newsId)
                    .build();

            firebaseMessaging.send(message);

            System.out.println("🔔 News notification sent: " + newsId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
