package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid", nullable = false)
    private long userid;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password",  nullable = false)
    private String password;

    @Column(name = "isactive")
    private boolean isactive;

    @Column(name = "phone")
    private String phone;

    @Column(name = "dateofbirth")
    private Date dateofbirth;

    @Column(name = "email",  nullable = false)
    private String email;

    @Column(name = "displayname")
    private String displayname;

    @Column(name = "created_at",   nullable = false)
    private Date created_at;

    @Column(name = "updated_at")
    private Date updated_at;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "userid"),
            inverseJoinColumns = @JoinColumn(name = "roleid")
    )
    private List<Role> roles;

    @OneToMany(mappedBy = "users")
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "users")
    private Carts carts;

    @OneToMany(mappedBy = "users")
    private List<cart_items> cart_items = new ArrayList<>();

    @OneToMany(mappedBy = "buyers")
    private List<Contracts> buyerContracts = new ArrayList<>();

    @OneToMany(mappedBy = "sellers")
    private List<Contracts> sellerContracts = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Feedback> feedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Post> posts = new ArrayList<>();;

    @OneToMany(mappedBy = "userReviewed")
    private List<Post> userReviewed = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy")
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "buyer")
    private List<Chatroom> chatroomBuyer = new ArrayList<>();

    @OneToMany(mappedBy = "seller")
    private List<Chatroom> chatroomSeller = new ArrayList<>();

    @OneToMany(mappedBy = "admins")
    private List<Contracts> contracts = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "users")
    private List<Favorite_post> favorite_posts = new ArrayList<>();

    @OneToMany(mappedBy = "buyerid")
    private List<Message> buyer = new ArrayList<>();

    @OneToMany(mappedBy = "sellerid")
    private List<Message> seller = new ArrayList<>();
}
