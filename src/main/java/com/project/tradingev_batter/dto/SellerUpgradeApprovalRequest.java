package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerUpgradeApprovalRequest {
    private boolean approved;
    private String rejectionReason; // Chỉ cần khi approved = false
}
