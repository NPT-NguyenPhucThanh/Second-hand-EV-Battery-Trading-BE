package com.project.tradingev_batter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long messageId;
    private Long chatroomId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE
    private Date timestamp;
    private boolean isRead;
}
