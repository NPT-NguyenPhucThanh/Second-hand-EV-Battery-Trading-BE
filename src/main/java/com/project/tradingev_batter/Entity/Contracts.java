package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "contracts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contracts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contractid")
    private long contractid;

    @Column(name = "signedat")
    private Date signedat;

    @Column(name = "signedbyBuyer")
    private Date signedbyBuyer;

    @Column(name = "signedbySeller")
    private Date signedbySeller;

    @Column(name = "signedMethod")
    private String signedMethod;

    @Column(name = "contractFile")
    private String contractFile;

    @Column(name = "startDate")
    private Date startDate;

    @Column(name = "endDate")
    private Date endDate;

    @Column(name = "status")
    private boolean status;

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "buyerid")
    private User buyers;

    @ManyToOne
    @JoinColumn(name = "sellerid")
    private User sellers;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User admins;
}
