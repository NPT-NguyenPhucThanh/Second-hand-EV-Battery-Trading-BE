package com.project.tradingev_batter.Entity;

import com.project.tradingev_batter.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "disputes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disputeid")
    private long disputeid;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "status", columnDefinition = "NVARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    @Column(name = "resolution", columnDefinition = "NVARCHAR(MAX)")
    private String resolution;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "resolved_at")
    private Date resolvedAt;

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User manager;
}
