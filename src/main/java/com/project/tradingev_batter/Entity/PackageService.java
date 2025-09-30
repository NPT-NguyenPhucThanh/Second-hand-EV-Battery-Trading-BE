package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "package_services")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PackageService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "packageid")
    private long packageid;

    @Column(name = "name")
    private String name; // e.g., "Cơ bản"

    @Column(name = "duration_months")
    private int durationMonths; // e.g., 1

    @Column(name = "price")
    private double price; // e.g., 100000

    @Column(name = "max_cars")
    private int maxCars; // e.g., 1

    @Column(name = "max_batteries")
    private int maxBatteries; // e.g., 2

    @Column(name = "created_at")
    private Date createdAt;
}