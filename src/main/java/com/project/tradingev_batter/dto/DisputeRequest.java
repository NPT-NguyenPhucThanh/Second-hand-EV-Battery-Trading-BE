package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotBlank(message = "Description không được để trống")
    @Size(min = 20, max = 1000, message = "Description phải từ 20-1000 ký tự")
    private String description;

    @NotBlank(message = "Loại lý do không được để trống")
    private String reasonType; // "WRONG_ITEM", "DAMAGED", "NOT_WORKING", "OTHER"
}
