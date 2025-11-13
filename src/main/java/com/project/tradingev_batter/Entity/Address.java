package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addressid")
    private long addressid;

    @Column(name = "street", columnDefinition = "NVARCHAR(255)")
    private String street;

    @Column(name = "ward", columnDefinition = "NVARCHAR(100)")
    private String ward;

    @Column(name = "district", columnDefinition = "NVARCHAR(100)")
    private String district;

    @Column(name = "province", columnDefinition = "NVARCHAR(100)")
    private String province;

    @Column(name = "country", columnDefinition = "NVARCHAR(100)")
    private String country;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;

    @JsonIgnore
    @OneToMany(mappedBy = "address")
    private List<Orders> ordersList = new ArrayList<>();
}
