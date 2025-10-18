package com.project.tradingev_batter.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 5, max = 200, message = "Tên sản phẩm phải từ 5 đến 200 ký tự")
    private String productname;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 10, max = 2000, message = "Mô tả phải từ 10 đến 2000 ký tự")
    private String description;

    @Positive(message = "Giá phải lớn hơn 0")
    private double cost;

    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    private int amount;

    private String status;
    private String model;

    @NotBlank(message = "Loại sản phẩm không được để trống")
    private String type;

    private String specs;
}
