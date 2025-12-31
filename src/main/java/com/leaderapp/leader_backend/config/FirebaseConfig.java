package com.leaderapp.leader_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

//@Configuration
//public class FirebaseConfig {
//
//    @PostConstruct
//    public void initialize() throws IOException {
//        if (FirebaseApp.getApps().isEmpty()) {
//            FileInputStream serviceAccount =
//                    new FileInputStream("src/main/resources/firebase-service-account.json");
//
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .build();
//
//            FirebaseApp.initializeApp(options);
//            System.out.println("🔥 Firebase initialized successfully");
//        }
//    }
//}


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
//import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            String firebaseJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");

            if (firebaseJson == null || firebaseJson.isEmpty()) {
                throw new RuntimeException("FIREBASE_SERVICE_ACCOUNT env variable not found");
            }

            InputStream serviceAccount =
                    new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (Exception e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}