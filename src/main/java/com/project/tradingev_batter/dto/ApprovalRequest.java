package com.project.tradingev_batter.dto;

import lombok.Data;

@Data
public class ApprovalRequest {
    private boolean approved;
    private String note;
}
