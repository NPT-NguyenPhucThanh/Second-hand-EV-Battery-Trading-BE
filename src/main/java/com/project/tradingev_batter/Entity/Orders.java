package com.project.tradingev_batter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.tradingev_batter.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderid")
    private long orderid;

    @Column(name = "total_amount")
    private double totalamount;

    @Column(name = "shipping_fee")
    private double shippingfee;

    @Column(name = "total_final")
    private double totalfinal;

    @Column(name = "shipping_address")
    private String shippingaddress;

    @Column(name = "paymentMethod")
    private String paymentmethod;

    @Column(name = "createdat")
    private Date createdat;

    @Column(name = "updatedat")
    private Date updatedat;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "package_id")
    private Long packageId; // Lưu ID gói dịch vụ khi order là mua gói

    @Column(name = "transaction_location")
    private String transactionLocation; // Điểm giao dịch (cho đơn xe)

    @Column(name = "appointment_date")
    private Date appointmentDate; // Thời gian hẹn giao dịch

    @Column(name = "transfer_ownership")
    private Boolean transferOwnership = false; // Có sang tên xe không

    @Column(name = "change_plate")
    private Boolean changePlate = false; // Có đổi biển số không

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Contracts>  contracts = new ArrayList<>();

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Feedback> feedbacks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyerid")
    private User users;

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order_detail> details = new ArrayList<>();

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Refund> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Chatroom> chatroom = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "addressid")
    private Address address;
}
