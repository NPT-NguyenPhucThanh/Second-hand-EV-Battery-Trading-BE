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
    private String signedMethod; // "DOCUSEAL"

    @Column(name = "contractFile")
    private String contractFile; // URL file hợp đồng PDF

    @Column(name = "startDate")
    private Date startDate;

    @Column(name = "endDate")
    private Date endDate;

    @Column(name = "status")
    private boolean status; // true = đã ký, false = chờ ký

    // DocuSeal Integration Fields
    @Column(name = "docuseal_submission_id")
    private String docusealSubmissionId; // ID submission từ DocuSeal

    @Column(name = "docuseal_document_url")
    private String docusealDocumentUrl; // URL document đã ký từ DocuSeal

    @Column(name = "seller_signed_at")
    private Date sellerSignedAt; // Thời gian seller ký

    @Column(name = "contract_type")
    private String contractType; // "PRODUCT_LISTING" (đăng bán) hoặc "SALE_TRANSACTION" (mua bán)

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "productid")
    private Product products;

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
