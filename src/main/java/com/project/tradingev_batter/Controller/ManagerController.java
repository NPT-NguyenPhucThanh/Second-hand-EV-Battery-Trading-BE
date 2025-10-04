package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.tradingev_batter.dto.ApprovalRequest;
import com.project.tradingev_batter.dto.LockRequest;
import com.project.tradingev_batter.Repository.PackageServiceRepository;
import com.project.tradingev_batter.dto.RefundRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {
    private final ManagerService managerService;
    private final PackageServiceRepository packageServiceRepository;

    public ManagerController(ManagerService managerService,
                             PackageServiceRepository packageServiceRepository) {
        this.managerService = managerService;
        this.packageServiceRepository = packageServiceRepository;
    }

    @GetMapping("/notifications/{managerId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long managerId) {
        return ResponseEntity.ok(managerService.getNotiForManager(managerId));
    }

    @PostMapping("/products/{productId}/approve-preliminary")
    public ResponseEntity<String> approvePreliminary(@PathVariable Long productId, @RequestBody ApprovalRequest request) {
        managerService.approvePreliminaryProduct(productId, request.getNote(), request.isApproved());
        return ResponseEntity.ok("Processed");
    }

    @PostMapping("/products/{productId}/input-inspection")
    public ResponseEntity<String> inputInspection(@PathVariable Long productId, @RequestBody ApprovalRequest request) {
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

    @PostMapping("/users/{userId}/approve-seller")
    public ResponseEntity<String> approveSeller(@PathVariable Long userId, @RequestBody ApprovalRequest request) {
        managerService.approveSellerUpgrade(userId, request.isApproved());
        return ResponseEntity.ok("Seller upgrade processed");
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
}
