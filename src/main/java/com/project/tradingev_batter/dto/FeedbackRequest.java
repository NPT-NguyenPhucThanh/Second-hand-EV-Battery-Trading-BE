package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @Min(value = 1, message = "Rating phải từ 1-5")
    @Max(value = 5, message = "Rating phải từ 1-5")
    private int rating;

    @NotBlank(message = "Comment không được để trống")
    @Size(min = 10, max = 1000, message = "Comment phải từ 10-1000 ký tự")
    private String comment;
}
