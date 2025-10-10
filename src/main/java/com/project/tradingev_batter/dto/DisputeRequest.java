package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRequest {
    private Long orderId;
    private String description;
    private String reasonType; // "WRONG_ITEM", "DAMAGED", "NOT_WORKING", "OTHER"
}
