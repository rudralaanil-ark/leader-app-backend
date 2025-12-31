package com.leaderapp.leader_backend.controllers;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.leaderapp.leader_backend.dtos.*;
import com.leaderapp.leader_backend.services.FirebaseService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


//package com.leaderapp.leader_backend.controllers;


import com.leaderapp.leader_backend.services.FirebaseService;
import com.leaderapp.leader_backend.dtos.CreateMonitorRequest;
import com.leaderapp.leader_backend.dtos.UpdateEmailRequest;
import com.leaderapp.leader_backend.dtos.UpdatePasswordRequest;
import com.leaderapp.leader_backend.dtos.SetStatusRequest;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*") // Allow Expo app in dev
public class AdminController {
    private final FirebaseService firebaseService;

    public AdminController(@Lazy FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @PostMapping("/create-monitor")
    public ResponseEntity<?> createMonitor(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateMonitorRequest body) throws Exception {

        String token = extractToken(authHeader);
        FirebaseToken decoded = firebaseService.verifyToken(token);
        String adminUid = decoded.getUid();

        var record = firebaseService.createMonitor(body.getEmail(), body.getPassword(), body.getFullName(), adminUid);
        return ResponseEntity.ok(Map.of("uid", record.getUid(), "email", record.getEmail()));
    }

    @PostMapping("/update-email")
    public ResponseEntity<?> updateEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEmailRequest body) throws FirebaseAuthException {

        firebaseService.verifyToken(extractToken(authHeader));
        firebaseService.updateEmail(body.getUid(), body.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "Email updated successfully"));
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdatePasswordRequest body) throws FirebaseAuthException {

        firebaseService.verifyToken(extractToken(authHeader));
        firebaseService.updatePassword(body.getUid(), body.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/set-status")
    public ResponseEntity<?> setStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SetStatusRequest body)
            throws ExecutionException, InterruptedException, FirebaseAuthException {

        firebaseService.verifyToken(extractToken(authHeader));
        firebaseService.setMonitorStatus(body.getUid(), body.isStatus());
        return ResponseEntity.ok(Map.of("message", "Monitor status updated"));
    }

    @GetMapping("/monitors")
    public ResponseEntity<?> getMonitors(
            @RequestHeader("Authorization") String authHeader)
            throws Exception {

        String token = extractToken(authHeader);
        FirebaseToken decoded = firebaseService.verifyToken(token);
        String adminUid = decoded.getUid();

        List<Map<String, Object>> monitors = firebaseService.fetchMonitorsByAdmin(adminUid);
        return ResponseEntity.ok(monitors);
    }

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return header.substring(7);
    }

    @DeleteMapping("/delete-monitor/{uid}")
    public ResponseEntity<?> deleteMonitor(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String uid) throws Exception {

        // Verify admin token
        firebaseService.verifyToken(extractToken(authHeader));

        // Delete monitor from Firebase and Firestore
        firebaseService.deleteMonitor(uid);

        return ResponseEntity.ok(Map.of("message", "Monitor deleted successfully"));
    }
}
