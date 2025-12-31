package com.leaderapp.leader_backend.notifications;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FcmService {

    public void sendToTopic(
            String topic,
            String title,
            String body,
            Map<String, String> data
    ) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ FCM sent successfully: " + response);

        } catch (Exception e) {
            System.err.println("❌ Failed to send FCM");
            e.printStackTrace();
        }
    }
}
