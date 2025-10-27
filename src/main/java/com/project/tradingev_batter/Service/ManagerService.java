package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.dto.RefundRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ManagerService {
    List<Notification> getNotiForManager(Long managerId);

    // Product Approval Methods
    List<Product> getPendingApprovalProducts(); // Lấy danh sách sản phẩm CHỜ DUYỆT
    List<Product> getPendingInspectionProducts(); // Lấy danh sách sản phẩm CHỜ KIỂM ĐỊNH
    void approvePreliminaryProduct(Long productId, String note, boolean approved);

    void inputInspectionResult(Long productId, boolean passed, String note);

    List<Product> getWarehouseProducts();

    void addToWarehouse(Long productId);

    List<com.project.tradingev_batter.Entity.Orders> getAllOrders(); // Lấy tất cả orders

    List<com.project.tradingev_batter.Entity.Orders> getOrdersByStatus(String status); // Lấy orders theo status

    void approveOrder(Long orderId, boolean approved, String note);

    void resolveDispute(Long disputeId, String resolution, RefundRequest refundRequest);

    // Seller Upgrade - Cập nhật signature
    void approveSellerUpgrade(Long userId, boolean approved, String rejectionReason);
    List<User> getPendingSellerUpgradeRequests(); // Danh sách yêu cầu pending

    void lockUser(Long userId, boolean lock); //Khóa/Mở khóa tài khoản người dùng

    PackageService createPackage(PackageService pkg); //Quản lý gói dịch vụ

    PackageService updatePackage(Long id, PackageService pkg); //Cập nhật gói dịch vụ

    Map<String, Object> getRevenueReport(); //Báo cáo doanh thu

    Map<String, Object> getSystemReport(); //Báo cáo hệ thống

    List<Product> getPendingWarehouseProducts(); //Sản phẩm chờ nhập kho

    void removeFromWarehouse(Long productId, String reason); //Xóa sản phẩm khỏi kho

    void updateWarehouseStatus(Long productId, String newStatus); //Cập nhật trạng thái kho

    void processOrderRefundIfRejected(Long orderId, double depositeAmount); //Xử lý hoàn tiền đơn hàng bị từ chối
}
