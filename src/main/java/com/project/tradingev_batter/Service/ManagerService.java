package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ManagerService {
    List<Notification> getNotiForManager(Long managerId);
    void approvePreliminaryProduct(Long productId, String note, boolean approved);
    void inputInspectionResult(Long productId, boolean passed, String note);
    List<Product> getWarehouseProducts();
    void addToWarehouse(Long productId);
    void approveOrder(Long orderId, boolean approved, String note);
    void resolveDispute(Long disputeId, String resolution);
}
