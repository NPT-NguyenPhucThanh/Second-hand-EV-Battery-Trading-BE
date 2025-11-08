package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.*;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff APIs", description = "API dành cho nhân viên - Duyệt sản phẩm, kiểm định, quản lý kho, xử lý giao dịch & tranh chấp")
public class StaffController {
    private final ManagerService managerService;
    private final DisputeService disputeService;
    private final RefundService refundService;
    private final UserService userService;

    public StaffController(ManagerService managerService,
                          DisputeService disputeService,
                          RefundService refundService,
                          UserService userService) {
        this.managerService = managerService;
        this.disputeService = disputeService;
        this.refundService = refundService;
        this.userService = userService;
    }

    // PRODUCT APPROVAL va` INSPECTION

    @Operation(summary = "Lấy danh sách thông báo cho nhân viên")
    @GetMapping("/notifications/{staffId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long staffId) {
        return ResponseEntity.ok(managerService.getNotiForManager(staffId));
    }

    @Operation(summary = "Lấy danh sách bài đăng cần duyệt",
               description = "Staff lấy danh sách tất cả sản phẩm xe đang ở trạng thái CHỜ DUYỆT")
    @GetMapping("/products/pending-approval")
    public ResponseEntity<Map<String, Object>> getPendingProducts() {
        List<Product> pendingProducts = managerService.getPendingApprovalProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalPending", pendingProducts.size());
        response.put("products", pendingProducts);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách sản phẩm đang chờ kiểm định",
               description = "Staff lấy danh sách tất cả sản phẩm xe đang ở trạng thái CHỜ KIỂM ĐỊNH")
    @GetMapping("/products/pending-inspection")
    public ResponseEntity<Map<String, Object>> getPendingInspectionProducts() {
        List<Product> pendingInspection = managerService.getPendingInspectionProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalPending", pendingInspection.size());
        response.put("products", pendingInspection);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Duyệt sản phẩm (giai đoạn sơ bộ)",
               description = "Staff duyệt sơ bộ thông tin sản phẩm xe trước khi chuyển đi kiểm định")
    @PostMapping("/products/{productId}/approve-preliminary")
    public ResponseEntity<String> approvePreliminary(
            @PathVariable Long productId,
            @Valid @RequestBody ApprovalRequest request) {
        managerService.approvePreliminaryProduct(productId, request.getNote(), request.isApproved());
        return ResponseEntity.ok("Processed");
    }

    @Operation(summary = "Nhập kết quả kiểm định sản phẩm",
               description = "Staff nhập kết quả kiểm định từ bên thứ ba vào hệ thống")
    @PostMapping("/products/{productId}/input-inspection")
    public ResponseEntity<String> inputInspection(
            @PathVariable Long productId,
            @Valid @RequestBody ApprovalRequest request) {
        managerService.inputInspectionResult(productId, request.isApproved(), request.getNote());
        return ResponseEntity.ok("Processed");
    }

    // WAREHOUSE MANAGEMENT

    @Operation(summary = "Lấy danh sách sản phẩm trong kho")
    @GetMapping("/warehouse")
    public ResponseEntity<List<Product>> getWarehouse() {
        return ResponseEntity.ok(managerService.getWarehouseProducts());
    }

    @Operation(summary = "Lấy danh sách sản phẩm trong kho đang chờ xử lý")
    @GetMapping("/warehouse/pending")
    public ResponseEntity<List<Product>> getPendingWarehouse() {
        return ResponseEntity.ok(managerService.getPendingWarehouseProducts());
    }

    @Operation(summary = "Thêm sản phẩm vào kho",
               description = "Staff thêm sản phẩm đã đạt kiểm định vào kho")
    @PostMapping("/warehouse/add/{productId}")
    public ResponseEntity<String> addToWarehouse(@PathVariable Long productId) {
        managerService.addToWarehouse(productId);
        return ResponseEntity.ok("Added to warehouse");
    }

    @Operation(summary = "Xóa sản phẩm khỏi kho")
    @PostMapping("/warehouse/remove/{productId}")
    public ResponseEntity<String> removeFromWarehouse(
            @PathVariable Long productId,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        managerService.removeFromWarehouse(productId, reason);
        return ResponseEntity.ok("Removed from warehouse");
    }

    @Operation(summary = "Cập nhật trạng thái sản phẩm trong kho")
    @PutMapping("/warehouse/{productId}/status")
    public ResponseEntity<String> updateWarehouseStatus(
            @PathVariable Long productId,
            @RequestParam String newStatus) {
        managerService.updateWarehouseStatus(productId, newStatus);
        return ResponseEntity.ok("Status updated to " + newStatus);
    }

    //  ORDER MANAGEMENT

    @Operation(summary = "Lấy danh sách tất cả đơn hàng",
               description = "Staff lấy danh sách tất cả đơn hàng trong hệ thống")
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        List<Orders> orders = managerService.getAllOrders();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalOrders", orders.size());
        response.put("orders", orders);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách đơn hàng theo trạng thái",
               description = "Staff lấy danh sách đơn hàng theo status (CHO_DUYET, DA_DUYET, DANG_GIAO_DICH, DA_HOAN_TAT, BI_TU_CHOI, TRANH_CHAP, etc.)")
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatus(@PathVariable String status) {
        List<Orders> orders = managerService.getOrdersByStatus(status);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("orderStatus", status);
        response.put("totalOrders", orders.size());
        response.put("orders", orders);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Duyệt đơn hàng",
               description = "Staff duyệt hoặc từ chối đơn hàng sau khi buyer đặt cọc")
    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<String> approveOrder(
            @PathVariable Long orderId,
            @RequestBody ApprovalRequest request) {
        managerService.approveOrder(orderId, request.isApproved(), request.getNote());
        return ResponseEntity.ok("Order processed");
    }

    // DISPUTE RESOLUTION

    @Operation(summary = "Giải quyết tranh chấp",
               description = "Staff xử lý tranh chấp giữa buyer và seller. Decision: APPROVE_REFUND hoặc REJECT_DISPUTE")
    @SuppressWarnings("unchecked")
    @PostMapping("/disputes/{disputeId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveDispute(
            @PathVariable Long disputeId,
            @RequestBody Map<String, Object> request) {

        String decision = (String) request.get("decision"); // APPROVE_REFUND hoặc REJECT_DISPUTE
        String managerNote = (String) request.get("managerNote");

        // Xử lý dispute
        Dispute resolvedDispute = disputeService.resolveDispute(disputeId, decision, managerNote);

        // Nếu quyết định hoàn tiền, tạo refund request
        if ("APPROVE_REFUND".equals(decision)) {
            Map<String, Object> refundMap = (Map<String, Object>) request.get("refund");
            if (refundMap != null) {
                Long orderId = ((Number) refundMap.get("orderId")).longValue();
                Double amount = (Double) refundMap.get("amount");
                String reason = (String) refundMap.get("reason");

                // Tạo refund
                Refund refund = refundService.createRefund(orderId, amount, reason);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Dispute đã được giải quyết và tạo refund thành công");
                response.put("dispute", resolvedDispute);
                response.put("refund", refund);
                return ResponseEntity.ok(response);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Dispute đã được giải quyết");
        response.put("dispute", resolvedDispute);
        return ResponseEntity.ok(response);
    }

    // REFUND MANAGEMENT

    @Operation(summary = "Lấy tất cả các yêu cầu hoàn tiền")
    @GetMapping("/refunds")
    public ResponseEntity<Map<String, Object>> getAllRefunds() {
        List<Refund> refunds = refundService.getAllRefunds();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalRefunds", refunds.size());
        response.put("refunds", refunds);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy các yêu cầu hoàn tiền đang chờ xử lý")
    @GetMapping("/refunds/pending")
    public ResponseEntity<Map<String, Object>> getPendingRefunds() {
        List<Refund> refunds = refundService.getRefundsByStatus(com.project.tradingev_batter.enums.RefundStatus.PENDING);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalPending", refunds.size());
        response.put("refunds", refunds);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy chi tiết một yêu cầu hoàn tiền")
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

    @Operation(summary = "Nhân viên xử lý yêu cầu hoàn tiền")
    @PostMapping("/refunds/{refundId}/process")
    public ResponseEntity<Map<String, Object>> processRefund(
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> request) {

        try {
            User staff = getCurrentUser();

            boolean approve = (Boolean) request.get("approve");
            String refundMethod = (String) request.getOrDefault("refundMethod", "VNPay");
            String note = (String) request.getOrDefault("note", "");

            if (approve && refundMethod == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Vui lòng chọn phương thức hoàn tiền"
                ));
            }

            Refund processedRefund = refundService.processRefund(
                refundId,
                staff.getUserid(),
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

    @Operation(summary = "Lấy danh sách yêu cầu hoàn tiền của một đơn hàng")
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

    // SELLER UPGRADE APPROVAL

    @Operation(summary = "Lấy danh sách yêu cầu nâng cấp của người bán")
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

    @Operation(summary = "Xét duyệt yêu cầu nâng cấp của người bán")
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

    // USER MANAGEMENT (VIEW ONLY)

    @Operation(summary = "Lấy danh sách tất cả người dùng",
               description = "Staff có thể xem danh sách users để tra cứu thông tin khi xử lý tranh chấp")
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
                "sellerUpgradeStatus", user.getSellerUpgradeStatus()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                "status", "error",
                "message", "Không tìm thấy user: " + e.getMessage()
            ));
        }
    }

    // USER PACKAGE MANAGEMENT
    @Operation(summary = "Lấy danh sách tất cả người dùng đang sử dụng gói",
               description = "Staff lấy danh sách tất cả user đang có gói package còn hiệu lực")
    @GetMapping("/user-packages/active")
    public ResponseEntity<Map<String, Object>> getAllActiveUserPackages() {
        try {
            List<UserPackage> userPackages = managerService.getAllActiveUserPackages();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalActivePackages", userPackages.size());
            response.put("userPackages", userPackages.stream().map(up -> {
                Map<String, Object> packageMap = new HashMap<>();
                packageMap.put("userPackageId", up.getUserpackageid());
                packageMap.put("userId", up.getUser().getUserid());
                packageMap.put("username", up.getUser().getUsername());
                packageMap.put("displayName", up.getUser().getDisplayname() != null ? up.getUser().getDisplayname() : "N/A");
                packageMap.put("email", up.getUser().getEmail());
                packageMap.put("packageId", up.getPackageService().getPackageid());
                packageMap.put("packageName", up.getPackageService().getName());
                packageMap.put("packageType", up.getPackageService().getPackageType());
                packageMap.put("purchaseDate", up.getPurchaseDate());
                packageMap.put("expiryDate", up.getExpiryDate());
                packageMap.put("remainingCars", up.getRemainingCars());
                packageMap.put("remainingBatteries", up.getRemainingBatteries());
                packageMap.put("durationMonths", up.getPackageService().getDurationMonths());
                return packageMap;
            }).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Không thể tải danh sách user packages: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Lấy danh sách người dùng đang sử dụng gói theo loại",
               description = "Staff lấy danh sách user đang có gói CAR hoặc BATTERY còn hiệu lực")
    @GetMapping("/user-packages/active/type/{packageType}")
    public ResponseEntity<Map<String, Object>> getActiveUserPackagesByType(@PathVariable String packageType) {
        try {
            // Validate packageType
            if (!"CAR".equals(packageType) && !"BATTERY".equals(packageType)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Package type phải là 'CAR' hoặc 'BATTERY'"
                ));
            }

            List<UserPackage> userPackages = managerService.getActiveUserPackagesByType(packageType);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("packageType", packageType);
            response.put("totalActivePackages", userPackages.size());
            response.put("userPackages", userPackages.stream().map(up -> {
                Map<String, Object> packageMap = new HashMap<>();
                packageMap.put("userPackageId", up.getUserpackageid());
                packageMap.put("userId", up.getUser().getUserid());
                packageMap.put("username", up.getUser().getUsername());
                packageMap.put("displayName", up.getUser().getDisplayname() != null ? up.getUser().getDisplayname() : "N/A");
                packageMap.put("email", up.getUser().getEmail());
                packageMap.put("packageId", up.getPackageService().getPackageid());
                packageMap.put("packageName", up.getPackageService().getName());
                packageMap.put("purchaseDate", up.getPurchaseDate());
                packageMap.put("expiryDate", up.getExpiryDate());
                packageMap.put("remainingCars", up.getRemainingCars());
                packageMap.put("remainingBatteries", up.getRemainingBatteries());
                packageMap.put("durationMonths", up.getPackageService().getDurationMonths());
                return packageMap;
            }).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Không thể tải danh sách user packages: " + e.getMessage()
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

