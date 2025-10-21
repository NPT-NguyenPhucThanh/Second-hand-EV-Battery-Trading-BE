package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import com.project.tradingev_batter.dto.ChatMessageDTO;
import org.springframework.data.domain.Page;

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
    
    // Pagination for messages
    Page<Message> getMessagesByChatroomPaginated(Long chatroomId, int page, int size);

    // Helper
    int getUnreadMessageCount(Long chatroomId, Long userId);
}
