package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Order_detail;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Transaction;

import java.util.List;

public interface OrderService {
    // Xem đơn hàng
    List<Orders> getOrders(long userid);
    List<Order_detail> getOrderDetails(long orderId, long userid) throws Exception;
    Orders getOrderById(Long orderId);
    
    // Tạo đơn hàng
    Orders createOrderFromProduct(Long userId, Long productId, int quantity, String shippingAddress, String paymentMethod);
    Orders createOrderFromCart(Long userId, String shippingAddress, String paymentMethod);
    
    // Kiểm tra loại đơn hàng
    boolean isCarOrder(Orders order);
    boolean isBatteryOrder(Orders order);
    
    // Xử lý thanh toán
    Transaction processDeposit(Long userId, Long orderId, String paymentMethod); // Đặt cọc 10%
    Transaction processFinalPayment(Long userId, Long orderId, String paymentMethod, boolean transferOwnership, boolean changePlate);
    
    // Xác nhận nhận hàng (cho pin)
    Orders confirmReceipt(Long userId, Long orderId);
}
