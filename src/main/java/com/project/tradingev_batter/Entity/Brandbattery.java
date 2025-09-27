package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brandbatteries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Brandbattery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batteryid")
    private long batteryid;

    @Column(name = "year")
    private int year;

    @Column(name = "capacity")
    private double capacity;

    @Column(name = "remaining_capacity")
    private double remaining;

    @OneToOne
    @JoinColumn(name = "productid")
    private Product products;
}
