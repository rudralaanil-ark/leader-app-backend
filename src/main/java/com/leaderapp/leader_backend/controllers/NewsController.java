package com.leaderapp.leader_backend.controllers;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.leaderapp.leader_backend.dtos.CreateNewsRequest;
import com.leaderapp.leader_backend.services.FirebaseService;
import com.leaderapp.leader_backend.services.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/news")
@CrossOrigin(origins = "*")
public class NewsController {

    private final NewsService newsService;
    private final FirebaseService firebaseService;

    public NewsController(NewsService newsService, FirebaseService firebaseService) {
        this.newsService = newsService;
        this.firebaseService = firebaseService;
    }

    // ✅ Add news
    @PostMapping("/add")
    public ResponseEntity<?> addNews(@RequestHeader("Authorization") String authHeader,
                                     @RequestBody CreateNewsRequest request)
            throws FirebaseAuthException, ExecutionException, InterruptedException {

        FirebaseToken token = firebaseService.verifyToken(authHeader.substring(7));

        Map<String, Object> data = new HashMap<>();
        data.put("title", request.getTitle());
        data.put("description", request.getDescription());
        data.put("imageUrl", request.getImageUrl());
        data.put("videoUrl", request.getVideoUrl());
        data.put("createdBy", token.getUid());
        data.put("createdAt", new Date().getTime());

        newsService.addNews(data);

        return ResponseEntity.ok(Map.of("message", "News added successfully"));
    }

    // ✅ Get all news
    @GetMapping("/all")
    public ResponseEntity<?> getAllNews() throws ExecutionException, InterruptedException {
        List<Map<String, Object>> news = newsService.getAllNews();
        return ResponseEntity.ok(news);
    }

    // ✅ Get news by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsById(@PathVariable String id)
            throws ExecutionException, InterruptedException {

        Map<String, Object> news = newsService.getNewsById(id);
        if (news == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "News not found"));
        return ResponseEntity.ok(news);
    }

    // ✅ Update news
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateNews(@PathVariable String id,
                                        @RequestBody Map<String, Object> updatedNews)
            throws ExecutionException, InterruptedException {

        boolean updated = newsService.updateNews(id, updatedNews);

        if (updated)
            return ResponseEntity.ok(Map.of("message", "News updated successfully"));
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "News not found"));
    }

    // ✅ Delete news
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteNews(@PathVariable String id)
            throws ExecutionException, InterruptedException {

        boolean deleted = newsService.deleteNews(id);
        if (deleted)
            return ResponseEntity.ok(Map.of("message", "News deleted successfully"));
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "News not found"));
    }
}
