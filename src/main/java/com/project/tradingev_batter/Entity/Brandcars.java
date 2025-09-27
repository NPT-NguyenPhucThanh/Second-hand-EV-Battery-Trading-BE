package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brandcars")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Brandcars {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carid")
    private long carid;

    @Column(name = "year")
    private int year;

    @Column(name = "odo")
    private double odo;

    @Column(name = "capacity")
    private double capacity;

    @Column(name = "color")
    private String color;

    @OneToOne
    @JoinColumn(name = "productid")
    private Product products;
}
