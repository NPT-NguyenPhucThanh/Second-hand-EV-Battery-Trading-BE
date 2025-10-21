package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.Refund;
import com.project.tradingev_batter.Service.ManagerService;
import com.project.tradingev_batter.Service.RefundService;
import com.project.tradingev_batter.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.project.tradingev_batter.dto.ApprovalRequest;
import com.project.tradingev_batter.dto.LockRequest;
import com.project.tradingev_batter.Repository.PackageServiceRepository;
import com.project.tradingev_batter.dto.RefundRequest;
import com.project.tradingev_batter.dto.SellerUpgradeApprovalRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {
    private final ManagerService managerService;
    private final PackageServiceRepository packageServiceRepository;
    private final RefundService refundService;

    public ManagerController(ManagerService managerService,
                             PackageServiceRepository packageServiceRepository,
                             RefundService refundService) {
        this.managerService = managerService;
        this.packageServiceRepository = packageServiceRepository;
        this.refundService = refundService;
    }

    @GetMapping("/notifications/{managerId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long managerId) {
        return ResponseEntity.ok(managerService.getNotiForManager(managerId));
    }

    @PostMapping("/products/{productId}/approve-preliminary")
    public ResponseEntity<String> approvePreliminary(@PathVariable Long productId, @Valid @RequestBody ApprovalRequest request) {
        managerService.approvePreliminaryProduct(productId, request.getNote(), request.isApproved());
        return ResponseEntity.ok("Processed");
    }

    @PostMapping("/products/{productId}/input-inspection")
    public ResponseEntity<String> inputInspection(@PathVariable Long productId, @Valid @RequestBody ApprovalRequest request) {
        managerService.inputInspectionResult(productId, request.isApproved(), request.getNote());
        return ResponseEntity.ok("Processed");
    }

    @GetMapping("/warehouse")
    public ResponseEntity<List<Product>> getWarehouse() {
        return ResponseEntity.ok(managerService.getWarehouseProducts());
    }

    @PostMapping("/warehouse/add/{productId}")
    public ResponseEntity<String> addToWarehouse(@PathVariable Long productId) {
        managerService.addToWarehouse(productId);
        return ResponseEntity.ok("Added to warehouse");
    }

    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<String> approveOrder(@PathVariable Long orderId, @RequestBody ApprovalRequest request) {
        managerService.approveOrder(orderId, request.isApproved(), request.getNote());
        return ResponseEntity.ok("Order processed");
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/disputes/{disputeId}/resolve")
    public ResponseEntity<String> resolveDispute(@PathVariable Long disputeId, @RequestBody Map<String, Object> request) {
        String resolution = (String) request.get("resolution");
        Map<String, Object> refundMap = (Map<String, Object>) request.get("refund");  // Optional refund
        RefundRequest refundRequest = null;
        if (refundMap != null) {
            refundRequest = new RefundRequest();
            refundRequest.setAmount((Double) refundMap.get("amount"));
            refundRequest.setReason((String) refundMap.get("reason"));
            refundRequest.setStatus((String) refundMap.get("status"));
        }
        managerService.resolveDispute(disputeId, resolution, refundRequest);
        return ResponseEntity.ok("Dispute resolved" + (refundRequest != null ? " with refund" : ""));
    }

    //Lấy danh sách yêu cầu nâng cấp
    @GetMapping("/seller-upgrade/requests")
    public ResponseEntity<Map<String, Object>> getPendingSellerUpgradeRequests() {
        List<User> pendingRequests = managerService.getPendingSellerUpgradeRequests();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalRequests", pendingRequests.size());
        response.put("requests", pendingRequests.stream().map(user -> Map.of(
                "userId", user.getUserid(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "N/A",
                "requestDate", user.getSellerUpgradeRequestDate(),
                "cccdFrontUrl", user.getCccdFrontUrl(),
                "cccdBackUrl", user.getCccdBackUrl()
        )).toList());
        
        return ResponseEntity.ok(response);
    }

    //Xét duyệt yêu cầu nâng cấp
    @PostMapping("/seller-upgrade/{userId}/approve")
    public ResponseEntity<Map<String, Object>> approveSellerUpgradeRequest(
            @PathVariable Long userId, 
            @RequestBody SellerUpgradeApprovalRequest request) {
        
        managerService.approveSellerUpgrade(userId, request.isApproved(), request.getRejectionReason());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", request.isApproved() ? "Yêu cầu đã được chấp nhận" : "Yêu cầu đã bị từ chối");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<String> lockUser(@PathVariable Long userId, @RequestBody LockRequest request) {
        managerService.lockUser(userId, request.isLock());
        return ResponseEntity.ok("User locked/unlocked");
    }

    @PostMapping("/packages")
    public ResponseEntity<PackageService> createPackage(@RequestBody PackageService pkg) {
        return ResponseEntity.ok(managerService.createPackage(pkg));
    }

    @PutMapping("/packages/{id}")
    public ResponseEntity<PackageService> updatePackage(@PathVariable Long id, @RequestBody PackageService pkg) {
        return ResponseEntity.ok(managerService.updatePackage(id, pkg));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<PackageService>> getAllPackages() {
        return ResponseEntity.ok(packageServiceRepository.findAll());
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<String> deletePackage(@PathVariable Long id) {
        packageServiceRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueReport() {
        return ResponseEntity.ok(managerService.getRevenueReport());
    }

    @GetMapping("/reports/system")
    public ResponseEntity<Map<String, Object>> getSystemReport() {
        return ResponseEntity.ok(managerService.getSystemReport());
    }

    @GetMapping("/warehouse/pending")
    public ResponseEntity<List<Product>> getPendingWarehouse() {
        return ResponseEntity.ok(managerService.getPendingWarehouseProducts());
    }

    @PostMapping("/warehouse/remove/{productId}")
    public ResponseEntity<String> removeFromWarehouse(@PathVariable Long productId, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        managerService.removeFromWarehouse(productId, reason);
        return ResponseEntity.ok("Removed from warehouse");
    }

    @PutMapping("/warehouse/{productId}/status")
    public ResponseEntity<String> updateWarehouseStatus(@PathVariable Long productId, @RequestParam String newStatus) {
        managerService.updateWarehouseStatus(productId, newStatus);
        return ResponseEntity.ok("Status updated to " + newStatus);
    }

    //Lấy tất cả refund requests
    @GetMapping("/refunds")
    public ResponseEntity<Map<String, Object>> getAllRefunds() {
        List<Refund> refunds = refundService.getAllRefunds();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalRefunds", refunds.size());
        response.put("refunds", refunds);

        return ResponseEntity.ok(response);
    }

    //Lấy refund requests đang chờ xử lý (PENDING)
    @GetMapping("/refunds/pending")
    public ResponseEntity<Map<String, Object>> getPendingRefunds() {
        List<Refund> refunds = refundService.getRefundsByStatus(com.project.tradingev_batter.enums.RefundStatus.PENDING);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalPending", refunds.size());
        response.put("refunds", refunds);

        return ResponseEntity.ok(response);
    }

    //Lấy chi tiết refund request
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<Map<String, Object>> getRefundDetail(@PathVariable Long refundId) {
        try {
            Refund refund = refundService.getRefundById(refundId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("refund", refund);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Manager xử lý refund request
    @PostMapping("/refunds/{refundId}/process")
    public ResponseEntity<Map<String, Object>> processRefund(
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> request) {

        try {
            // Lấy current manager
            User manager = getCurrentUser();

            boolean approve = (Boolean) request.get("approve");
            String refundMethod = (String) request.getOrDefault("refundMethod", "VNPay");
            String note = (String) request.getOrDefault("note", "");

            // Validate refund method nếu approve
            if (approve && refundMethod == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Vui lòng chọn phương thức hoàn tiền"
                ));
            }

            Refund processedRefund = refundService.processRefund(
                refundId,
                manager.getUserid(),
                refundMethod,
                approve,
                note
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", approve ? "Đã chấp nhận hoàn tiền" : "Đã từ chối yêu cầu hoàn tiền");
            response.put("refund", processedRefund);

            if (approve) {
                response.put("refundAmount", processedRefund.getAmount());
                response.put("refundMethod", processedRefund.getRefundMethod());
                response.put("processedAt", processedRefund.getProcessedAt());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Lấy refunds của một order cụ thể
    @GetMapping("/refunds/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getRefundsByOrder(@PathVariable Long orderId) {
        try {
            List<Refund> refunds = refundService.getRefundsByOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("orderId", orderId);
            response.put("totalRefunds", refunds.size());
            response.put("refunds", refunds);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Dashboard tổng quan cho Manager
    //Hiển thị: pending tasks, recent activities, revenue, market trends
    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        try {
            Map<String, Object> systemReport = managerService.getSystemReport();
            Map<String, Object> revenueReport = managerService.getRevenueReport();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("status", "success");

            // Pending Tasks Summary
            Map<String, Object> pendingTasks = new HashMap<>();
            pendingTasks.put("pendingApprovalProducts", systemReport.get("pendingApprovalProducts"));
            pendingTasks.put("pendingInspectionProducts", systemReport.get("pendingInspectionProducts"));
            pendingTasks.put("pendingOrders", systemReport.get("pendingOrders"));
            pendingTasks.put("openDisputes", systemReport.get("openDisputes"));
            pendingTasks.put("pendingSellerUpgrades", managerService.getPendingSellerUpgradeRequests().size());
            dashboard.put("pendingTasks", pendingTasks);

            // Revenue Summary
            Map<String, Object> revenueSummary = new HashMap<>();
            revenueSummary.put("totalRevenue", revenueReport.get("totalRevenue"));
            revenueSummary.put("platformRevenue", revenueReport.get("platformRevenue"));
            revenueSummary.put("totalCommission", revenueReport.get("totalCommission"));
            revenueSummary.put("packageRevenue", revenueReport.get("packageRevenue"));
            dashboard.put("revenueSummary", revenueSummary);

            // Market Trends
            Map<String, Object> marketTrends = new HashMap<>();
            marketTrends.put("trendingCategory", systemReport.get("trendingCategory"));
            marketTrends.put("mostViewedProduct", systemReport.get("mostViewedProduct"));
            marketTrends.put("totalCarViews", systemReport.get("totalCarViews"));
            marketTrends.put("totalBatteryViews", systemReport.get("totalBatteryViews"));
            dashboard.put("marketTrends", marketTrends);

            // Quick Stats
            Map<String, Object> quickStats = new HashMap<>();
            quickStats.put("totalUsers", systemReport.get("totalUsers"));
            quickStats.put("totalProducts", systemReport.get("totalProducts"));
            quickStats.put("totalOrders", systemReport.get("totalOrders"));
            quickStats.put("completedOrders", systemReport.get("completedOrders"));
            dashboard.put("quickStats", quickStats);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Không thể tải dashboard: " + e.getMessage()
            ));
        }
    }

    // ============= HELPER METHODS ====================================================================================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
