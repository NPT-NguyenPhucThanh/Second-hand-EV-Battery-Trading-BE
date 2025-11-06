package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Carts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartsid")
    private long cartsid;

    @Column(name = "createdat")
    private Date createdat;

    @Column(name = "updatedat")
    private Date updatedat;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userid")
    @JsonIgnoreProperties({"carts", "orders", "packages", "products", "feedback", "disputes", "transactions"})
    private User users;

    @OneToMany(mappedBy = "carts", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"carts"}) // Tránh circular reference: carts -> cart_items -> carts
    private List<cart_items> cart_items = new ArrayList<>();
}
