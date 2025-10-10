package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import com.project.tradingev_batter.dto.ChatMessageDTO;

import java.util.List;

public interface ChatService {
    // Quản lý Chatroom
    Chatroom getOrCreateChatroom(Long buyerId, Long sellerId, Long orderId);
    Chatroom getChatroomById(Long chatroomId);
    List<Chatroom> getChatroomsByUser(Long userId);
    
    // Quản lý Messages
    Message saveMessage(ChatMessageDTO messageDTO);
    List<Message> getMessagesByChatroom(Long chatroomId);
    void markMessagesAsRead(Long chatroomId, Long userId);
    
    // Helper
    int getUnreadMessageCount(Long chatroomId, Long userId);
}
