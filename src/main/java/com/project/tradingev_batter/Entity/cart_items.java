package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "cart_items")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class cart_items {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itemsid")
    private long itemsid;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "addedat")
    private Date addedat;

    @ManyToOne
    @JoinColumn(name = "cartsid")
    private Carts carts;

    @ManyToOne
    @JoinColumn(name = "productid")
    private Product products;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;
}
