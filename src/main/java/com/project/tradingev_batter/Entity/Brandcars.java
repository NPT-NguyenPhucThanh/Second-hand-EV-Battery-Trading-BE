package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brandcars")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
public class Brandcars {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carid")
    private long carid;

    @Column(name = "brand")
    private String brand; // Hãng xe

    @Column(name = "year")
    private int year;

    @Column(name = "license_plate")
    private String licensePlate; // Biển số xe

    @Column(name = "odo")
    private double odo; // Số km đã đi

    @Column(name = "capacity")
    private double capacity; // Dung tích pin

    @Column(name = "color")
    private String color;

    @OneToOne
    @JoinColumn(name = "productid")
    private Product products;
}
