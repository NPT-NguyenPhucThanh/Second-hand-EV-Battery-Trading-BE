package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.NotificationService;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notification APIs", description = "API quản lý thông báo - Xem và xóa thông báo")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Lay danh sach notifications cua user hien tai
    @Operation(
            summary = "Lấy tất cả thông báo của user",
            description = "Lấy danh sách tất cả thông báo, sắp xếp từ mới đến cũ"
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserNotifications() {
        User currentUser = getCurrentUser();

        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getUserid());

        // Filter out notifications với created_time null, sau đó sort
        List<Notification> validNotifications = notifications.stream()
                .filter(n -> n.getCreated_time() != null)
                .sorted((n1, n2) -> n2.getCreated_time().compareTo(n1.getCreated_time())) // Mới nhất trước
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("notifications", validNotifications.stream().map(n -> {
            Map<String, Object> notifMap = new HashMap<>();
            notifMap.put("notificationId", n.getNotificationid());
            notifMap.put("title", n.getTitle() != null ? n.getTitle() : "");
            notifMap.put("description", n.getDescription() != null ? n.getDescription() : "");
            notifMap.put("createdTime", n.getCreated_time()); // Already filtered null above
            return notifMap;
        }).collect(Collectors.toList()));
        response.put("totalCount", validNotifications.size());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Xóa thông báo",
            description = "Xóa một thông báo"
    )
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "ID của thông báo", required = true)
            @PathVariable Long notificationId) {
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
