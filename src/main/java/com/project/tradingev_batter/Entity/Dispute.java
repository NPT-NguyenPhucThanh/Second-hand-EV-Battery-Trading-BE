package com.project.tradingev_batter.Entity;

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

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status; // e.g., "OPEN", "RESOLVED"

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "created_at")
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "orderid")
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User manager;
}
