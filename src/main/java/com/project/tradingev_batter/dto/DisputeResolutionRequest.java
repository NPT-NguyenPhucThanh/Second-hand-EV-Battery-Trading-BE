package com.project.tradingev_batter.dto;

import lombok.Data;

@Data
public class DisputeResolutionRequest {
    private String decision; // "APPROVE_REFUND" hoặc "REJECT_DISPUTE"
    private String managerNote; // Ghi chú của manager
}

