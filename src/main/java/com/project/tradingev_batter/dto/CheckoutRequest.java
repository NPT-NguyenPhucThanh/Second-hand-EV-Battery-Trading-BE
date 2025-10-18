package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @NotNull(message = "Product ID không được để trống")
    private Long productId; // Cho mua ngay

    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity; // Cho mua ngay

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // "VnPay", "COD", "Banking"
}
