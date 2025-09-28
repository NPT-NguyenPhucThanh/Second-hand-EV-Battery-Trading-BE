package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "chatroom")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chatroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatid")
    private long chatid;

    @Column(name = "createdat")
    private Date createdat;

    @ManyToOne
    @JoinColumn(name = "buyerid")
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "sellerid")
    private User seller;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders orders;

    @OneToMany(mappedBy = "chatroom")
    private List<Message> message = new ArrayList<>();
}
