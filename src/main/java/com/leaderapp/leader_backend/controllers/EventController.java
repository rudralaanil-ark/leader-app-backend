package com.leaderapp.leader_backend.controllers;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.leaderapp.leader_backend.services.EventService;
import com.leaderapp.leader_backend.services.FirebaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final FirebaseService firebaseService;

    public EventController(EventService eventService, FirebaseService firebaseService) {
        this.eventService = eventService;
        this.firebaseService = firebaseService;
    }

    // ✅ Add Event
    @PostMapping("/add")
    public ResponseEntity<?> addEvent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request
    ) throws FirebaseAuthException, ExecutionException, InterruptedException {

        FirebaseToken token = firebaseService.verifyToken(authHeader.substring(7));

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", request.get("title"));
        eventData.put("description", request.get("description"));
        eventData.put("location", request.get("location"));
        eventData.put("eventDate", request.get("eventDate"));
        eventData.put("imageUrl", request.get("imageUrl"));
        eventData.put("createdBy", token.getUid());
        eventData.put("createdAt", new Date().getTime());

        eventService.addEvent(eventData);

        return ResponseEntity.ok(Map.of("message", "Event added successfully!"));
    }

    // ✅ Get All Events
    @GetMapping("/all")
    public ResponseEntity<?> getAllEvents() throws ExecutionException, InterruptedException {
        List<Map<String, Object>> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    // ✅ Update Event
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String id,
            @RequestBody Map<String, Object> request
    ) throws ExecutionException, InterruptedException {
        boolean updated = eventService.updateEvent(id, request);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Event updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Event not found"));
        }
    }

    // ✅ Delete Event
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable String id)
            throws ExecutionException, InterruptedException {
        boolean deleted = eventService.deleteEvent(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Event not found"));
        }
    }


    // --- RSVP endpoints (requires Authorization header with Bearer token) ---
    @PostMapping("/rsvp/{id}")
    public ResponseEntity<?> addRsvp(@RequestHeader("Authorization") String auth, @PathVariable String id)
            throws FirebaseAuthException, ExecutionException, InterruptedException {
        FirebaseToken token = firebaseService.verifyToken(auth.substring(7));
        boolean added = eventService.addRsvp(id, token.getUid());
        if (added) return ResponseEntity.ok(Map.of("message","RSVP added"));
        else return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Already RSVPed"));
    }

    @DeleteMapping("/rsvp/{id}")
    public ResponseEntity<?> removeRsvp(@RequestHeader("Authorization") String auth, @PathVariable String id)
            throws FirebaseAuthException, ExecutionException, InterruptedException {
        FirebaseToken token = firebaseService.verifyToken(auth.substring(7));
        boolean removed = eventService.removeRsvp(id, token.getUid());
        if (removed) return ResponseEntity.ok(Map.of("message","RSVP removed"));
        else return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","RSVP not found"));
    }

    @GetMapping("/rsvps/{id}")
    public ResponseEntity<?> getRsvps(@PathVariable String id) throws ExecutionException, InterruptedException {
        int count = eventService.getRsvpCount(id);
        List<Map<String, Object>> users = eventService.getRsvpUserDetails(id);
        return ResponseEntity.ok(Map.of("count", count, "users", users));
    }


    @GetMapping("/rsvps/{id}/status")
    public ResponseEntity<?> rsvpStatus(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable String id)
            throws FirebaseAuthException, ExecutionException, InterruptedException {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("isGoing", false));
        }
        FirebaseToken token = firebaseService.verifyToken(auth.substring(7));
        boolean isGoing = eventService.isUserRsvped(id, token.getUid());
        return ResponseEntity.ok(Map.of("isGoing", isGoing));
    }

}
