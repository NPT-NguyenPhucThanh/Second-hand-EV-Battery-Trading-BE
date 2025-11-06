package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnore // Tránh circular reference: cart_items -> carts -> cart_items
    private Carts carts;

    @ManyToOne
    @JoinColumn(name = "productid")
    @JsonIgnoreProperties({"cart_items", "order_detail", "feedback", "images"})
    private Product products;

    @ManyToOne
    @JoinColumn(name = "userid")
    @JsonIgnoreProperties({"carts", "orders", "packages", "products", "feedback", "disputes", "transactions"})
    private User users;
}
