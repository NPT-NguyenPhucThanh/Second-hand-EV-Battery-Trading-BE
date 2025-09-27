package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "imgs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class product_img {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imgid")
    private long imgid;

    @Column(name = "url")
    private String url;

    @ManyToOne
    @JoinColumn(name = "productid")
    private Product products;
}
