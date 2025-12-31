package com.leaderapp.leader_backend.notifications;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationTestController {

    private final FcmService fcmService;

    public NotificationTestController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @GetMapping("/test")
    public String sendTestNotification() {

        Map<String, String> data = new HashMap<>();
        data.put("type", "test");
        data.put("message", "Hello from backend");

        fcmService.sendToTopic(
                "all_users",
                "🚀 Leader App Test",
                "Push notifications are working!",
                data
        );

        return "Test notification sent";
    }
}
