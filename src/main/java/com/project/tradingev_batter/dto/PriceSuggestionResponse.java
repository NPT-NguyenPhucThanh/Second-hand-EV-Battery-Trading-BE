package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceSuggestionResponse {

    private Double minPrice;
    private Double maxPrice;
    private Double suggestedPrice;
    private String marketInsight; // Thông tin thị trường từ Gemini
    private String currency;

    public PriceSuggestionResponse(Double minPrice, Double maxPrice, Double suggestedPrice, String marketInsight) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.suggestedPrice = suggestedPrice;
        this.marketInsight = marketInsight;
        this.currency = "VND";
    }
}

