package com.project.tradingev_batter.Entity;
import com.project.tradingev_batter.Entity.User;
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

    @Column(name = "street")
    private String street;

    @Column(name = "ward")
    private String ward;

    @Column(name = "district")
    private String district;

    @Column(name = "province")
    private String province;

    @Column(name = "country")
    private String country;
    
    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;

    @OneToMany(mappedBy = "address")
    private List<Order> orderList = new ArrayList<>();
}
