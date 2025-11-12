package com.project.tradingev_batter.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Refund;
import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.PackageServiceRepository;
import com.project.tradingev_batter.Service.DisputeService;
import com.project.tradingev_batter.Service.ManagerService;
import com.project.tradingev_batter.Service.RefundService;
import com.project.tradingev_batter.Service.UserService;
import com.project.tradingev_batter.dto.DisputeResolutionRequest;
import com.project.tradingev_batter.dto.LockRequest;
import com.project.tradingev_batter.dto.RefundProcessRequest;
import com.project.tradingev_batter.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/manager")
@Tag(name = "Manager APIs", description = "API dành cho quản trị viên cấp cao - Quản lý hệ thống, users, gói dịch vụ, doanh thu & báo cáo")
public class ManagerController {

    private final ManagerService managerService;
    private final PackageServiceRepository packageServiceRepository;
    private final UserService userService;
    private final DisputeService disputeService;
    private final RefundService refundService;

    public ManagerController(ManagerService managerService,
            PackageServiceRepository packageServiceRepository,
            UserService userService,
            DisputeService disputeService,
            RefundService refundService) {
        this.managerService = managerService;
        this.packageServiceRepository = packageServiceRepository;
        this.userService = userService;
        this.disputeService = disputeService;
        this.refundService = refundService;
    }

    // USER MANAGEMENT
    @Operation(summary = "Lấy danh sách tất cả người dùng")
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalUsers", users.size());
            response.put("users", users.stream().map(user -> Map.of(
                    "userId", user.getUserid(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayname() != null ? user.getDisplayname() : "N/A",
                    "phone", user.getPhone() != null ? user.getPhone() : "N/A",
                    "isActive", user.isIsactive(),
                    "roles", user.getRoles().stream().map(Role::getRolename).toList()
            )).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Không thể tải danh sách users: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Lấy chi tiết thông tin người dùng")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("user", Map.of(
                    "userId", user.getUserid(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayname() != null ? user.getDisplayname() : "N/A",
                    "phone", user.getPhone() != null ? user.getPhone() : "N/A",
                    "isActive", user.isIsactive(),
                    "roles", user.getRoles().stream().map(Role::getRolename).toList(),
                    "createdAt", user.getCreated_at(),
                    "sellerUpgradeStatus", user.getSellerUpgradeStatus() != null ? user.getSellerUpgradeStatus() : "N/A"
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Không tìm thấy user: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Khóa hoặc mở khóa người dùng",
            description = "Manager có thể khóa/mở khóa tài khoản người dùng")
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<String> lockUser(@PathVariable Long userId, @RequestBody LockRequest request) {
        managerService.lockUser(userId, request.isLock());
        return ResponseEntity.ok("User locked/unlocked");
    }

    // PACKAGE SERVICE MANAGEMENT
    @Operation(summary = "Tạo gói dịch vụ mới")
    @PostMapping("/packages")
    public ResponseEntity<PackageService> createPackage(@RequestBody PackageService pkg) {
        return ResponseEntity.ok(managerService.createPackage(pkg));
    }

    @Operation(summary = "Cập nhật thông tin gói dịch vụ")
    @PutMapping("/packages/{id}")
    public ResponseEntity<PackageService> updatePackage(@PathVariable Long id, @RequestBody PackageService pkg) {
        return ResponseEntity.ok(managerService.updatePackage(id, pkg));
    }

    @Operation(summary = "Lấy tất cả các gói dịch vụ")
    @GetMapping("/packages")
    public ResponseEntity<List<PackageService>> getAllPackages() {
        return ResponseEntity.ok(packageServiceRepository.findAll());
    }

    @Operation(summary = "Xóa gói dịch vụ")
    @DeleteMapping("/packages/{id}")
    public ResponseEntity<String> deletePackage(@PathVariable Long id) {
        packageServiceRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // REVENUE & REPORTS
    @Operation(summary = "Lấy báo cáo doanh thu",
            description = "Báo cáo tổng doanh thu từ xe, pin, gói dịch vụ, hoa hồng")
    @GetMapping("/reports/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueReport() {
        return ResponseEntity.ok(managerService.getRevenueReport());
    }

    @Operation(summary = "Lấy báo cáo hệ thống",
            description = "Báo cáo số lượng sản phẩm, đơn hàng, giao dịch, xu hướng thị trường")
    @GetMapping("/reports/system")
    public ResponseEntity<Map<String, Object>> getSystemReport() {
        return ResponseEntity.ok(managerService.getSystemReport());
    }

    // DISPUTE MANAGEMENT
    @Operation(summary = "Lấy danh sách tất cả disputes",
            description = "Lấy tất cả các khiếu nại/tranh chấp trong hệ thống")
    @GetMapping("/disputes")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllDisputes() {
        try {
            List<Dispute> disputes = managerService.getAllDisputes();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalDisputes", disputes.size());
            response.put("disputes", disputes.stream().map(dispute -> {
                Map<String, Object> disputeData = new HashMap<>();
                disputeData.put("disputeId", dispute.getDisputeid());

                if (dispute.getOrder() != null) {
                    Orders order = dispute.getOrder();
                    disputeData.put("orderId", order.getOrderid());

                    // Lấy buyer từ order
                    User buyer = order.getUsers();
                    if (buyer != null) {
                        disputeData.put("buyerId", buyer.getUserid());
                        disputeData.put("buyerName", buyer.getDisplayname() != null ? buyer.getDisplayname() : buyer.getUsername());
                    }

                    // Lấy seller từ detail đầu tiên
                    if (order.getDetails() != null && !order.getDetails().isEmpty()) {
                        User seller = order.getDetails().get(0).getProducts().getUsers();
                        if (seller != null) {
                            disputeData.put("sellerId", seller.getUserid());
                            disputeData.put("sellerName", seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername());
                        }
                    }
                }

                disputeData.put("description", dispute.getDescription());
                disputeData.put("status", dispute.getStatus() != null ? dispute.getStatus().name() : "N/A");
                disputeData.put("resolution", dispute.getResolution());
                disputeData.put("createdAt", dispute.getCreatedAt());
                disputeData.put("resolvedBy", dispute.getManager() != null ? dispute.getManager().getUsername() : null);

                return disputeData;
            }).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Không thể tải disputes: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Giải quyết dispute",
            description = "Manager giải quyết khiếu nại: APPROVE_REFUND (hoàn tiền) hoặc REJECT_DISPUTE (từ chối)")
    @PostMapping("/disputes/{disputeId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveDispute(
            @PathVariable Long disputeId,
            @RequestBody DisputeResolutionRequest request) {
        try {
            Dispute resolvedDispute = disputeService.resolveDispute(
                    disputeId,
                    request.getDecision(),
                    request.getManagerNote()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Khiếu nại đã được xử lý");
            response.put("disputeId", resolvedDispute.getDisputeid());
            response.put("disputeStatus", resolvedDispute.getStatus().name());
            response.put("resolvedAt", resolvedDispute.getResolvedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Không thể giải quyết dispute: " + e.getMessage()
            ));
        }
    }

    // REFUND MANAGEMENT
    @Operation(summary = "Lấy danh sách tất cả refund requests",
            description = "Lấy tất cả yêu cầu hoàn tiền trong hệ thống")
    @GetMapping("/refunds")
    public ResponseEntity<Map<String, Object>> getAllRefunds() {
        try {
            List<Refund> refunds = refundService.getAllRefunds();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalRefunds", refunds.size());
            response.put("refunds", refunds.stream().map(refund -> {
                Map<String, Object> refundData = new HashMap<>();
                refundData.put("refundId", refund.getRefundid());

                if (refund.getOrders() != null) {
                    Orders order = refund.getOrders();
                    refundData.put("orderId", order.getOrderid());

                    // Buyer info
                    User buyer = order.getUsers();
                    if (buyer != null) {
                        refundData.put("buyerId", buyer.getUserid());
                        refundData.put("buyerName", buyer.getDisplayname() != null ? buyer.getDisplayname() : buyer.getUsername());
                    }
                }

                refundData.put("amount", refund.getAmount());
                refundData.put("reason", refund.getReason());
                refundData.put("status", refund.getStatus() != null ? refund.getStatus().name() : "N/A");
                refundData.put("refundMethod", refund.getRefundMethod());
                refundData.put("createdAt", refund.getCreatedat());
                refundData.put("processedAt", refund.getProcessedAt());
                refundData.put("processedBy", refund.getProcessedBy() != null ? refund.getProcessedBy().getUsername() : null);

                return refundData;
            }).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Không thể tải refund requests: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Xử lý refund request",
            description = "Manager duyệt hoặc từ chối yêu cầu hoàn tiền")
    @PostMapping("/refunds/{refundId}/process")
    public ResponseEntity<Map<String, Object>> processRefund(
            @PathVariable Long refundId,
            @RequestBody RefundProcessRequest request) {
        try {
            User currentManager = getCurrentUser();

            Refund processedRefund = refundService.processRefund(
                    refundId,
                    currentManager.getUserid(),
                    request.getRefundMethod(),
                    request.isApprove(),
                    request.getNote()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", request.isApprove() ? "Refund đã được chấp nhận" : "Refund đã bị từ chối");
            response.put("refundId", processedRefund.getRefundid());
            response.put("refundStatus", processedRefund.getStatus().name());
            response.put("processedAt", processedRefund.getProcessedAt());
            response.put("amount", processedRefund.getAmount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Không thể xử lý refund: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Lấy chi tiết refund request",
            description = "Lấy thông tin chi tiết của một yêu cầu hoàn tiền")
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<Map<String, Object>> getRefundDetail(@PathVariable Long refundId) {
        try {
            Refund refund = refundService.getRefundById(refundId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");

            Map<String, Object> refundData = new HashMap<>();
            refundData.put("refundId", refund.getRefundid());
            refundData.put("amount", refund.getAmount());
            refundData.put("reason", refund.getReason());
            refundData.put("status", refund.getStatus().name());
            refundData.put("refundMethod", refund.getRefundMethod());
            refundData.put("createdAt", refund.getCreatedat());
            refundData.put("processedAt", refund.getProcessedAt());

            if (refund.getOrders() != null) {
                Orders order = refund.getOrders();
                refundData.put("orderId", order.getOrderid());
                refundData.put("orderStatus", order.getStatus().name());
                refundData.put("orderTotal", order.getTotalfinal());

                // Buyer info
                User buyer = order.getUsers();
                if (buyer != null) {
                    refundData.put("buyer", Map.of(
                            "userId", buyer.getUserid(),
                            "username", buyer.getUsername(),
                            "displayName", buyer.getDisplayname() != null ? buyer.getDisplayname() : "N/A",
                            "email", buyer.getEmail()
                    ));
                }
            }

            if (refund.getProcessedBy() != null) {
                refundData.put("processedBy", Map.of(
                        "userId", refund.getProcessedBy().getUserid(),
                        "username", refund.getProcessedBy().getUsername()
                ));
            }

            response.put("refund", refundData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Không tìm thấy refund: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Lấy thông tin tổng quan cho dashboard của quản lý",
            description = "Dashboard hiển thị: pending tasks, revenue summary, market trends, quick stats")
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
