package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private Long productId; // Cho mua ngay
    private Integer quantity; // Cho mua ngay
    private String shippingAddress;
    private String paymentMethod; // "VnPay", "COD", "Banking"
}
