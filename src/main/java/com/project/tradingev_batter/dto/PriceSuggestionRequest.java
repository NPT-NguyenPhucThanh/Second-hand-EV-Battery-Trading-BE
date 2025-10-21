package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceSuggestionRequest {

    @NotBlank(message = "Product type is required (CAR or BATTERY)")
    private String productType; // CAR or BATTERY

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "Condition is required (NEW or USED)")
    private String condition; // NEW or USED

    //thêm các trường tùy ý
    private String model;
    private Double capacity; // cho battery
    private Integer mileage; // cho car
}

