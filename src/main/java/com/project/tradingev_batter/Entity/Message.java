package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "messid")
    private long messid;

    @Column(name = "text")
    private String text;

    @Column(name = "attachUrl")
    private String attachUrl;

    @Column(name = "createdat")
    private Date createdat;

    @ManyToOne
    @JoinColumn(name = "roomid")
    private Chatroom  chatroom;

    @ManyToOne
    @JoinColumn(name = "buyerid")
    private User buyerid;

    @ManyToOne
    @JoinColumn(name = "sellerid")
    private User sellerid;
}
