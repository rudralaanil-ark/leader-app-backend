package com.leaderapp.leader_backend.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.leaderapp.leader_backend.services.S3UploadService;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final S3UploadService uploadService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public MediaController(S3UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /* ===================== PRESIGN UPLOAD ===================== */

    @PostMapping("/presign")
    public Map<String, String> generatePresignedUrl(
            @RequestParam String fileName
    ) {

        String uploadUrl =
                uploadService.generatePresignedUploadUrl(fileName);

        String fileUrl =
                "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;

        return Map.of(
                "uploadUrl", uploadUrl,
                "fileUrl", fileUrl
        );
    }

    /* ===================== PRESIGN VIEW (OPTIONAL) ===================== */

    @GetMapping("/view")
    public Map<String, String> getViewUrl(@RequestParam String key) {

        String viewUrl = uploadService.generatePresignedViewUrl(key);

        return Map.of("viewUrl", viewUrl);
    }

    /* ===================== DELETE VIDEO ===================== */

    @PostMapping("/delete")
    public Map<String, Object> deleteVideo(@RequestBody Map<String, String> body) {

        try {
            String key = body.get("key"); // videos/{userId}/{postId}.mp4

            if (key == null || key.isEmpty()) {
                return Map.of("ok", false, "error", "key is required");
            }

            uploadService.deleteObject(key);

            return Map.of("ok", true);

        } catch (Exception e) {
            return Map.of(
                    "ok", false,
                    "error", e.getMessage()
            );
        }
    }
}
