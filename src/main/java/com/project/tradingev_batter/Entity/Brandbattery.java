package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brandbatteries")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
public class Brandbattery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batteryid")
    private long batteryid;

    @Column(name = "brand", columnDefinition = "NVARCHAR(100)")
    private String brand;

    @Column(name = "year")
    private int year;

    @Column(name = "capacity")
    private double capacity;

    @Column(name = "voltage")
    private double voltage;

    @Column(name = "condition", columnDefinition = "NVARCHAR(100)")
    private String condition; // "Mới", "Cũ", "Đã sử dụng"

    @Column(name = "pickup_address", columnDefinition = "NVARCHAR(500)")
    private String pickupAddress; // Địa chỉ lấy hàng

    @Column(name = "remaining_capacity")
    private double remaining;

    @OneToOne
    @JoinColumn(name = "productid")
    private Product products;
}
