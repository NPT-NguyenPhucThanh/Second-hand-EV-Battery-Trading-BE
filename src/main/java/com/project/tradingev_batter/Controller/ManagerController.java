package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.project.tradingev_batter.dto.ApprovalRequest;

import java.util.List;

@RestController
@RequestMapping("/api/manager")

public class ManagerController {
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
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
    public ResponseEntity<String> resolveDispute(@PathVariable Long disputeId, @RequestBody ApprovalRequest request) {
        managerService.resolveDispute(disputeId, request.getNote());
        return ResponseEntity.ok("Dispute resolved");
    }
}
