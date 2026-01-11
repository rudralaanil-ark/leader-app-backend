package com.leaderapp.leader_backend.controllers;

import org.springframework.beans.factory.annotation.Value;
import com.leaderapp.leader_backend.services.NewsImageUploadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads/news")
public class NewsUploadController {

    private final NewsImageUploadService uploadService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public NewsUploadController(NewsImageUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/image")
    public Map<String, Object> uploadNewsImage(
            @RequestParam("newsId") String newsId,
            @RequestParam("file") MultipartFile file
    ) {

        if (newsId == null || newsId.isBlank()) {
            return Map.of(
                    "success", false,
                    "error", "newsId is required"
            );
        }

        String key = uploadService.uploadNewsImage(newsId, file);

        String url = "https://" + bucket + ".s3." + region +
                ".amazonaws.com/" + key;

        return Map.of(
                "success", true,
                "key", key,
                "url", url
        );
    }


    @DeleteMapping("/image")
    public Map<String, Object> deleteNewsImage(
            @RequestParam("key") String key
    ) {

        uploadService.deleteNewsImage(key);

        return Map.of(
                "success", true,
                "message", "News image deleted successfully"
        );
    }

}
