package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@Table(name = "message")
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "messid")
    private Long messid;

    @Column(name = "text", columnDefinition = "NVARCHAR(MAX)")
    private String text;

    @Column(name = "attachUrl")
    private String attachUrl;

    @Column(name = "message_type", columnDefinition = "NVARCHAR(50)")
    private String messageType; // TEXT, IMAGE, FILE

    @Column(name = "is_read")
    private boolean isRead; // Đã đọc chưa

    @Column(name = "createdat")
    private Date createdat;

    @ManyToOne
    @JoinColumn(name = "roomid")
    private Chatroom chatroom;

    @ManyToOne
    @JoinColumn(name = "sender_id") // Người gửi (có thể là buyer hoặc seller)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "buyerid")
    private User buyerid;

    @ManyToOne
    @JoinColumn(name = "sellerid")
    private User sellerid;
}
