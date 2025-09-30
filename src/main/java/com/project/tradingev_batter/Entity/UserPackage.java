package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "user_packages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userpackageid")
    private long userpackageid;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    @ManyToOne
    @JoinColumn(name = "packageid")
    private PackageService packageService;

    @Column(name = "purchase_date")
    private Date purchaseDate;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "remaining_cars")
    private int remainingCars;

    @Column(name = "remaining_batteries")
    private int remainingBatteries;
}