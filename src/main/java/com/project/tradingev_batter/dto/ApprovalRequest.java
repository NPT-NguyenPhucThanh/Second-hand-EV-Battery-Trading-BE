package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequest {
    @NotNull(message = "Trạng thái duyệt không được để trống")
    private boolean approved;

    private String note;
}
