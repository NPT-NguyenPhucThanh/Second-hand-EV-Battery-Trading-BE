package com.project.tradingev_batter.Entity;

import com.project.tradingev_batter.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(name = "refund")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refundid")
    private long refundid;

    @Column(name = "amount")
    private double amount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @CreationTimestamp //auto save current timestamp
    @Column(name = "createdat")
    private Date createdat;

    @Column(name = "processed_at")
    private Date processedAt; // Thời điểm xử lý refund

    @Column(name = "refund_method")
    private String refundMethod; // Phương thức hoàn tiền: "VNPay", "Bank Transfer", "Cash"

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy; // Manager xử lý refund

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders orders;
}
