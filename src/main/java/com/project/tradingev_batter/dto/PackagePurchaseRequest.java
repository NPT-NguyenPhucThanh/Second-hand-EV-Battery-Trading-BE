package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagePurchaseRequest {
    @NotNull(message = "Package ID không được để trống")
    private Long packageId;
}
