package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolutionRequest {
    @NotBlank(message = "Resolution không được để trống")
    private String resolution;
}
