package com.leaderapp.leader_backend.services;

import com.leaderapp.leader_backend.media.MediaEntityType;
import com.leaderapp.leader_backend.media.ImageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaUploadService {

    private final S3Client s3Client;

    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024; // 20MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif"
    );

    @Value("${aws.s3.bucket}")
    private String bucket;

    public MediaUploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /* ===================== UPLOAD (OLD – STILL SUPPORTED) ===================== */
    // Backward compatible (single image per entity)

    public String uploadImage(
            MediaEntityType entityType,
            String entityId,
            MultipartFile file
    ) {
        return uploadImage(entityType, entityId, ImageType.COVER, file);
    }

    /* ===================== UPLOAD (NEW – COVER / GALLERY) ===================== */

    public String uploadImage(
            MediaEntityType entityType,
            String entityId,
            ImageType imageType,
            MultipartFile file
    ) {

        if (entityId == null || entityId.isBlank()) {
            throw new RuntimeException("entityId is required");
        }

        String contentType = validateAndResolveContentType(file);
        String extension = contentType.substring(contentType.indexOf("/") + 1);

        String key = buildS3Key(entityType, entityId, imageType, extension);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );

            return key;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload media to S3", e);
        }
    }

    /* ===================== DELETE SINGLE ===================== */

    public void deleteMedia(MediaEntityType entityType, String key) {

        if (key == null || key.isBlank()) {
            throw new RuntimeException("S3 key is required");
        }

        if (!key.startsWith(entityType.getFolder() + "/")) {
            throw new RuntimeException("Invalid delete path");
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete media from S3", e);
        }
    }

    /* ===================== DELETE MULTIPLE (BY PREFIX) ===================== */

    public int deleteAllByEntity(MediaEntityType entityType, String entityId) {

        if (entityId == null || entityId.isBlank()) {
            throw new RuntimeException("entityId is required");
        }

        String prefix = entityType.getFolder() + "/" + entityId + "/";

        try {
            int deletedCount = 0;
            String continuationToken = null;

            do {
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .continuationToken(continuationToken)
                        .build();

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

                if (listResponse.contents() == null || listResponse.contents().isEmpty()) {
                    break;
                }

                List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                        .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                        .toList();

                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();

                s3Client.deleteObjects(deleteRequest);

                deletedCount += objectsToDelete.size();
                continuationToken = listResponse.nextContinuationToken();

            } while (continuationToken != null);

            return deletedCount;

        } catch (Exception e) {
            throw new RuntimeException("Failed to bulk delete media", e);
        }
    }

    /* ===================== DELETE MULTIPLE (BY KEYS) ===================== */

    public int deleteByKeys(MediaEntityType entityType, List<String> keys) {

        if (keys == null || keys.isEmpty()) {
            throw new RuntimeException("keys are required");
        }

        for (String key : keys) {
            if (!key.startsWith(entityType.getFolder() + "/")) {
                throw new RuntimeException("Invalid delete key: " + key);
            }
        }

        try {
            List<ObjectIdentifier> objects = keys.stream()
                    .map(k -> ObjectIdentifier.builder().key(k).build())
                    .toList();

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objects).build())
                    .build();

            s3Client.deleteObjects(request);

            return objects.size();

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete selected media", e);
        }
    }

    /* ===================== INTERNAL ===================== */

    private String buildS3Key(
            MediaEntityType entityType,
            String entityId,
            ImageType imageType,
            String extension
    ) {
        return switch (entityType) {

            case NEWS, EVENTS -> {
                if (imageType == ImageType.COVER) {
                    yield entityType.getFolder() + "/" + entityId +
                            "/cover." + extension;
                } else {
                    yield entityType.getFolder() + "/" + entityId +
                            "/gallery/" + UUID.randomUUID() + "." + extension;
                }
            }

            case PROFILE ->
                    entityType.getFolder() + "/" + entityId + "." + extension;

            case GALLERY, COMPLAINT ->
                    entityType.getFolder() + "/" + entityId + "/" +
                            UUID.randomUUID() + "." + extension;
        };
    }

    private String validateAndResolveContentType(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Image size must be less than 20MB");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType != null && contentType.startsWith("image/")) {
            return contentType;
        }

        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("Invalid image file");
        }

        String extension =
                filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Unsupported image format");
        }

        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "heic", "heif" -> "image/heic";
            default -> "image/jpeg";
        };
    }
}
