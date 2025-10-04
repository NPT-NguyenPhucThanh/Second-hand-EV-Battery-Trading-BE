package com.project.tradingev_batter.dto;

import lombok.Data;

@Data
public class RefundRequest {
    private double amount;
    private String reason;
    private String status;
}
