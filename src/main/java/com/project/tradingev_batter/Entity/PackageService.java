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

    @Column(name = "name", columnDefinition = "NVARCHAR(100)")
    private String name; //"Cơ bản", "Chuyên nghiệp", "VIP"

    @Column(name = "package_type", columnDefinition = "NVARCHAR(50)")
    private String packageType; // "CAR" hoặc "BATTERY"

    @Column(name = "duration_months")
    private int durationMonths; //1, 6, 12

    @Column(name = "price")
    private double price;

    @Column(name = "max_cars")
    private int maxCars; //Số xe tối đa (nếu packageType = "CAR")

    @Column(name = "max_batteries")
    private int maxBatteries; // Số pin tối đa (nếu packageType = "BATTERY")

    @Column(name = "created_at")
    private Date createdAt;
    
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description; // Mô tả gói
}
