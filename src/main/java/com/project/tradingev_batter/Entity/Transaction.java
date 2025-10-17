package com.project.tradingev_batter.Entity;

import com.project.tradingev_batter.enums.TransactionStatus;
import com.project.tradingev_batter.enums.TransactionType;
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
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "createdat")
    private Date createdat;

    // ============ VNPay Integration Fields ============
    
    @Column(name = "amount")
    private Double amount; // Số tiền giao dịch
    
    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // DEPOSIT, FINAL_PAYMENT, BATTERY_PAYMENT, PACKAGE_PURCHASE
    
    @Column(name = "transaction_code", unique = true)
    private String transactionCode; // Mã giao dịch nội bộ (order_id + timestamp)
    
    @Column(name = "vnpay_transaction_no")
    private String vnpayTransactionNo; // Mã giao dịch từ VNPay
    
    @Column(name = "bank_code")
    private String bankCode; // Ngân hàng thanh toán (NCB, VISA, ...)
    
    @Column(name = "card_type")
    private String cardType; // ATM, QRCODE, ...
    
    @Column(name = "vnpay_response_code")
    private String vnpayResponseCode; // 00 = success, khác = failed
    
    @Column(name = "payment_date")
    private Date paymentDate; // Thời gian thanh toán thực tế từ VNPay
    
    @Column(name = "escrow_release_date")
    private Date escrowReleaseDate; // Ngày giải phóng tiền (7 ngày sau payment_date)
    
    @Column(name = "is_escrowed")
    private Boolean isEscrowed = false; // Tiền đang được giữ 7 ngày?
    
    @Column(name = "description", length = 500)
    private String description; // Mô tả giao dịch
    
    // ============ Relationships ============

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
