package com.project.tradingev_batter.Entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.tradingev_batter.enums.ProductStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cart_item", "order_detail", "posts"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "productid")
    private long productid;

    @Column(name = "productname", columnDefinition = "NVARCHAR(255)")
    private String productname;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "cost")
    private double cost;

    @Column(name = "amount")
    private int amount;

    @Column(name = "status", columnDefinition = "NVARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "model", columnDefinition = "NVARCHAR(100)")
    private String model;

    //Two-wheel EV, Car EV, Battery
    @Column(name = "type", columnDefinition = "NVARCHAR(50)")
    private String type;

    //Thong so ki thuat
    @Column(name = "specs", columnDefinition = "NVARCHAR(MAX)")
    private String specs;

    @Column(name = "createdat")
    private Date createdat;

    @Column(name = "updatedat")
    private Date updatedat;

    @Column(name = "in_warehouse")
    private Boolean inWarehouse = false;

    @Column(name = "view_count")
    private Integer viewCount = 0; // Đếm lượt xem sản phẩm

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userid")
    @JsonIgnoreProperties({"roles", "products", "password", "identityCard", "vehicleRegistration", "isActive", "upgradeStatus", "upgradeRequestedAt"})
    private User users;

    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<product_img> imgs = new ArrayList<>();

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Brandcars brandcars;

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Brandbattery brandbattery;

    @OneToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<cart_items> cart_item = new ArrayList<>();

    @OneToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Feedback> feedbacks = new ArrayList<>();

    @OneToOne(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Post posts;

    @OneToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order_detail> order_detail = new ArrayList<>();

    @OneToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Contracts> contracts = new ArrayList<>();
}
