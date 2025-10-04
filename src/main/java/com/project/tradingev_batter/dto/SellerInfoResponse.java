package com.project.tradingev_batter.dto;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.Product;
import lombok.Data;

import java.util.List;

@Data
public class SellerInfoResponse {
    private String username;
    private String displayname;
    private List<Product> products;

    public SellerInfoResponse(String username, String displayname, List<Product> products) {
        this.username = username;
        this.displayname = displayname;
        this.products = products;
    }
}
