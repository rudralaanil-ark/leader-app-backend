package com.leaderapp.leader_backend.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class NewsService {

    private Firestore getDb() {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase not initialized");
        }
        return FirestoreClient.getFirestore();
    }

    // ✅ Add News
    public void addNews(Map<String, Object> newsData)
            throws ExecutionException, InterruptedException {
        getDb().collection("news").add(newsData).get();
    }

    // ✅ Get all news
    public List<Map<String, Object>> getAllNews()
            throws ExecutionException, InterruptedException {

        var docs = getDb()
                .collection("news")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments();

        List<Map<String, Object>> newsList = new ArrayList<>();
        for (var doc : docs) {
            Map<String, Object> data = doc.getData();
            data.put("id", doc.getId());
            newsList.add(data);
        }
        return newsList;
    }

    public Map<String, Object> getNewsById(String id)
            throws ExecutionException, InterruptedException {

        DocumentSnapshot doc =
                getDb().collection("news").document(id).get().get();

        if (!doc.exists()) return null;

        Map<String, Object> data = doc.getData();
        data.put("id", doc.getId());
        return data;
    }

    public boolean updateNews(String id, Map<String, Object> updates)
            throws ExecutionException, InterruptedException {

        DocumentReference docRef =
                getDb().collection("news").document(id);

        DocumentSnapshot doc = docRef.get().get();
        if (!doc.exists()) return false;

        updates.put("updatedAt", System.currentTimeMillis());
        docRef.update(updates).get();
        return true;
    }

    public boolean deleteNews(String id)
            throws ExecutionException, InterruptedException {

        DocumentReference docRef =
                getDb().collection("news").document(id);

        DocumentSnapshot doc = docRef.get().get();
        if (!doc.exists()) return false;

        docRef.delete().get();
        return true;
    }
}
