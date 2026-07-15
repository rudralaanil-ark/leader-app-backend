

package com.leaderapp.leader_backend.controllers;

import com.leaderapp.leader_backend.media.MediaEntityType;
import com.leaderapp.leader_backend.media.ImageType;
import com.leaderapp.leader_backend.services.MediaUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads/media")
public class MediaUploadController {

    private final MediaUploadService mediaService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public MediaUploadController(MediaUploadService mediaService) {
        this.mediaService = mediaService;
    }

    /* ===================== UPLOAD (BACKWARD COMPATIBLE) ===================== */
    // Existing clients continue to work (defaults to COVER)

    @PostMapping
    public Map<String, Object> uploadMedia(
            @RequestParam("entityType") String entityTypeStr,
            @RequestParam("entityId") String entityId,
            @RequestParam(value = "imageType", required = false) String imageTypeStr,
            @RequestParam("file") MultipartFile file
    ) {

        // Parse entity type
        MediaEntityType entityType;
        try {
            entityType = MediaEntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entityType: " + entityTypeStr);
        }

        // Parse image type (default = COVER)
        ImageType imageType = ImageType.COVER;
        if (imageTypeStr != null && !imageTypeStr.isBlank()) {
            try {
                imageType = ImageType.valueOf(imageTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid imageType: " + imageTypeStr);
            }
        }

        String key = mediaService.uploadImage(
                entityType,
                entityId,
                imageType,
                file
        );

        String url = "https://" + bucket + ".s3." + region +
                ".amazonaws.com/" + key;

        return Map.of(
                "success", true,
                "key", key,
                "url", url
        );
    }

    /* ===================== DELETE SINGLE ===================== */

    @DeleteMapping
    public Map<String, Object> deleteMedia(
            @RequestParam("entityType") String entityTypeStr,
            @RequestParam("key") String key
    ) {

        MediaEntityType entityType;
        try {
            entityType = MediaEntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entityType: " + entityTypeStr);
        }

        mediaService.deleteMedia(entityType, key);

        return Map.of(
                "success", true,
                "message", "Media deleted successfully"
        );
    }

    /* ===================== DELETE ALL MEDIA FOR ENTITY ===================== */

    @DeleteMapping("/all")
    public Map<String, Object> deleteAllMediaForEntity(
            @RequestParam("entityType") String entityTypeStr,
            @RequestParam("entityId") String entityId
    ) {

        MediaEntityType entityType;
        try {
            entityType = MediaEntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entityType: " + entityTypeStr);
        }

        int deletedCount = mediaService.deleteAllByEntity(entityType, entityId);

        return Map.of(
                "success", true,
                "deletedCount", deletedCount
        );
    }

    /* ===================== DELETE SELECTED KEYS ===================== */

    @DeleteMapping("/batch")
    public Map<String, Object> deleteSelectedMedia(
            @RequestParam("entityType") String entityTypeStr,
            @RequestBody List<String> keys
    ) {

        MediaEntityType entityType;
        try {
            entityType = MediaEntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entityType: " + entityTypeStr);
        }

        int deletedCount = mediaService.deleteByKeys(entityType, keys);

        return Map.of(
                "success", true,
                "deletedCount", deletedCount
        );
    }
}

