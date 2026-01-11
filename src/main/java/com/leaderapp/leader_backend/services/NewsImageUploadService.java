//package com.leaderapp.leader_backend.services;
//import org.springframework.beans.factory.annotation.Value;
//import jakarta.annotation.PostConstruct;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//
//import java.util.Set;
//
//@Service
//public class NewsImageUploadService {
//
//    private final S3Client s3Client;
//    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024; // 5MB
//    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
//            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif"
//    );
//
//    @Value("${aws.s3.bucket}")
//    private String bucket;
//
//    public NewsImageUploadService(S3Client s3Client) {
//        this.s3Client = s3Client;
//    }
//
//    @PostConstruct
//    public void checkBucket() {
//        System.out.println("S3 BUCKET FROM CONFIG = " + bucket);
//    }
//
//    public String uploadNewsImage(String newsId, MultipartFile file) {
//
//        // 1️⃣ Validate
//        if (file.isEmpty()) {
//            throw new RuntimeException("File is empty");
//        }
//
//        if (!file.getContentType().startsWith("image/")) {
//            throw new RuntimeException("Only image uploads are allowed");
//        }
//
//        // 2️⃣ Extract extension
//        String contentType = file.getContentType();
//        String extension = contentType.substring(contentType.indexOf("/") + 1);
//
//        // 3️⃣ Build S3 key
//        String key = "news/" + newsId + "/image." + extension;
//
//        try {
//            // 4️⃣ Upload to S3
//            PutObjectRequest request = PutObjectRequest.builder()
//                    .bucket(bucket)
//                    .key(key)
//                    .contentType(contentType)
//                    .build();
//
//            s3Client.putObject(
//                    request,
//                    RequestBody.fromInputStream(
//                            file.getInputStream(),
//                            file.getSize()
//                    )
//            );
//
//            return key;
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to upload image to S3", e);
//        }
//    }
//}
//


package com.leaderapp.leader_backend.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;


import java.util.Set;

@Service
public class NewsImageUploadService {

    private final S3Client s3Client;


    // 🔒 Validation rules
    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024; // 20 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif"
    );

    @Value("${aws.s3.bucket}")
    private String bucket;

    public NewsImageUploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void checkBucket() {
        System.out.println("S3 BUCKET FROM CONFIG = " + bucket);
    }

    /* ===================== PUBLIC API ===================== */

    public String uploadNewsImage(String newsId, MultipartFile file) {

        if (newsId == null || newsId.isBlank()) {
            throw new RuntimeException("newsId is required");
        }

        String contentType = validateAndResolveContentType(file);

        String extension = contentType.substring(contentType.indexOf("/") + 1);
        String key = "news/" + newsId + "/image." + extension;

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
            throw new RuntimeException("Failed to upload image to S3", e);
        }
    }

    /* ===================== VALIDATION ===================== */

    private String validateAndResolveContentType(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Image size must be less than 20MB");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        // ✅ Case 1: Proper image content-type (best case)
        if (contentType != null && contentType.startsWith("image/")) {
            return contentType;
        }

        // ✅ Case 2: Fallback to extension (Postman / mobile safety)
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

    public void deleteNewsImage(String key) {

        if (key == null || key.isBlank()) {
            throw new RuntimeException("S3 key is required");
        }

        // Safety: allow delete ONLY inside news/
        if (!key.startsWith("news/")) {
            throw new RuntimeException("Invalid delete path");
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image from S3", e);
        }
    }

}
