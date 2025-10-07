package com.project.tradingev_batter.dto;

import lombok.Data;

@Data
public class ProductRequest {
    private String productname;
    private String description;
    private double cost;
    private int amount;
    private String status;
    private String model;
    private String type;
    private String specs;
}
