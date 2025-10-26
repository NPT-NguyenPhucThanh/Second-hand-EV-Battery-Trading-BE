package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private Long productId; // Cho mua ngay (optional - nếu null thì checkout from cart)

    private Integer quantity; // Cho mua ngay (optional)

    private String shippingAddress; // Địa chỉ giao hàng (cho pin)

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // "VnPay", "COD", "Banking"

    // Điểm giao dịch (optional - chỉ cần khi deposit xe)
    private String transactionLocation; // Địa điểm gặp mặt để giao dịch xe

    // Thời gian hẹn giao dịch (optional - chỉ cần khi deposit xe)
    // Validation @Future sẽ được check trong API /orders/{id}/deposit
    private Date appointmentDate; // Ngày giờ hẹn giao dịch

    // Có muốn sang tên xe không
    private Boolean transferOwnership = false;

    // Có muốn đổi biển số không
    private Boolean changePlate = false;
}
