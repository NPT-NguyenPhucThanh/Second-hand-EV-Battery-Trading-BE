package com.project.tradingev_batter.dto;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.Product;
import lombok.Data;

import java.util.List;

@Data
public class ProductDetailResponse {
    private Product product;
    private List<Feedback> feedbacks;
    public ProductDetailResponse(Product product, List<Feedback> feedbacks) {
        this.product = product;
        this.feedbacks = feedbacks;
    }
}
