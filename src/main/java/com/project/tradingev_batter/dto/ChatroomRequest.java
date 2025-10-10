package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatroomRequest {
    private Long orderId; // Chat về đơn hàng cụ thể
    private Long sellerId; // Chat với seller cụ thể
    private Long productId; // Chat về sản phẩm cụ thể
}
