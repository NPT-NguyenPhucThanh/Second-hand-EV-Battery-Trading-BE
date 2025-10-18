package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.NotificationService;
import com.project.tradingev_batter.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Lay danh sach notifications cua user hien tai
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserNotifications() {
        User currentUser = getCurrentUser();

        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getUserid());

        // Sap xep theo thoi gian moi nhat
        notifications.sort((n1, n2) -> n2.getCreated_time().compareTo(n1.getCreated_time()));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("notifications", notifications.stream().map(n -> Map.of(
                "notificationId", n.getNotificationid(),
                "title", n.getTitle(),
                "description", n.getDescription(),
                "createdTime", n.getCreated_time()
        )).collect(Collectors.toList()));
        response.put("totalCount", notifications.size());

        return ResponseEntity.ok(response);
    }

    // Xoa mot notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notification deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to delete notification: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //======= HELPER METHOD ============================================================================================
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser();
    }
}

