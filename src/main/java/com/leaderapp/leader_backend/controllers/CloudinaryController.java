package com.leaderapp.leader_backend.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @Autowired
    private Cloudinary cloudinary;

    // -------------------------------------------------
    // DELETE SINGLE IMAGE BY publicId
    // -------------------------------------------------
    @PostMapping("/delete")
    public Map<String, Object> deleteSingle(@RequestBody Map<String, String> body) {
        try {
            String publicId = body.get("publicId");
            if (publicId == null || publicId.isEmpty()) {
                return Map.of("ok", false, "error", "publicId is required");
            }

            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            return Map.of("ok", true, "result", result);

        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // -------------------------------------------------
    // DELETE ALL IMAGES UNDER A PREFIX (FOLDER)
    // Example prefix: gallery/userId/folderId
    // -------------------------------------------------
    @PostMapping("/delete-prefix")
    public Map<String, Object> deleteByPrefix(@RequestBody Map<String, String> body) {
        try {
            String prefix = body.get("prefix");
            if (prefix == null || prefix.isEmpty()) {
                return Map.of("ok", false, "error", "prefix is required");
            }

            // Step 1: Fetch all resources under prefix
            Map<?, ?> listResult = cloudinary.api().resources(
                    ObjectUtils.asMap(
                            "type", "upload",
                            "prefix", prefix,
                            "max_results", 500
                    )
            );

            List<Map<String, Object>> resources =
                    (List<Map<String, Object>>) listResult.get("resources");

            List<String> ids = new ArrayList<>();

            for (Map<String, Object> r : resources) {
                String publicId = (String) r.get("public_id");
                if (publicId != null) ids.add(publicId);
            }

            if (ids.isEmpty()) {
                return Map.of("ok", true, "deleted", List.of());
            }

            // Step 2: Delete all matching public IDs
            Map<?, ?> deleteResult = cloudinary.api().deleteResources(
                    ids,
                    ObjectUtils.asMap("resource_type", "image")
            );

            return Map.of("ok", true, "deleted", deleteResult);

        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}
