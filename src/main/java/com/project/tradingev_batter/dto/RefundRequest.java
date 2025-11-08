package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @Positive(message = "Số tiền phải lớn hơn 0")
    private double amount;

    @NotBlank(message = "Lý do hoàn tiền không được để trống")
    @Size(min = 20, max = 500, message = "Lý do phải từ 20-500 ký tự")
    private String reason;
}
