package com.project.tradingev_batter.Entity;

import com.project.tradingev_batter.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "productid")
    private long productid;

    @Column(name = "productname")
    private String productname;

    @Column(name = "description")
    private String description;

    @Column(name = "cost")
    private double cost;

    @Column(name = "amount")
    private int amount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "model")
    private String model;

    //Two-wheel EV, Car EV, Battery
    @Column(name = "type")
    private String type;

    //Thong so ki thuat
    @Column(name = "specs")
    private String specs;

    @Column(name = "createdat")
    private Date createdat;

    @Column(name = "updatedat")
    private Date updatedat;

    @Column(name = "in_warehouse")
    private Boolean inWarehouse = false;

    @Column(name = "view_count")
    private Integer viewCount = 0; // Đếm lượt xem sản phẩm

    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;

    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<product_img> imgs = new ArrayList<>();

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private Brandcars brandcars;

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private Brandbattery  brandbattery;

    @OneToMany(mappedBy = "products")
    private List<cart_items> cart_item = new ArrayList<>();

    @OneToMany(mappedBy = "products")
    private List<Feedback> feedbacks = new ArrayList<>();

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private Post posts;

    @OneToMany(mappedBy = "products")
    private List<Order_detail> order_detail = new ArrayList<>();
}
