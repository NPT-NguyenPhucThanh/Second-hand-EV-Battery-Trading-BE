package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transid")
    private long transid;

    @Column(name = "method")
    private String method;

    @Column(name = "status")
    private String status;

    @Column(name = "createdat")
    private Date createdat;

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Order orders;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
