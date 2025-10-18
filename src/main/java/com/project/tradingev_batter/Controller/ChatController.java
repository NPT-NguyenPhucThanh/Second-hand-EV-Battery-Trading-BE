package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.ChatService;
import com.project.tradingev_batter.Service.NotificationService;
import com.project.tradingev_batter.dto.ChatMessageDTO;
import com.project.tradingev_batter.dto.ChatroomRequest;
import com.project.tradingev_batter.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChatController - Xử lý WebSocket cho chat real-time và REST API cho quản lý chatroom
 * WebSocket Flow:
 * 1. Client connect: ws://localhost:8080/ws-chat
 * 2. Client subscribe: /topic/chatroom/{chatroomId}
 * 3. Client send: /app/chat.sendMessage
 * 4. Server broadcast: /topic/chatroom/{chatroomId}
 */
@Controller
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ChatController(ChatService chatService,
                         SimpMessagingTemplate messagingTemplate,
                         NotificationService notificationService) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    // ============= REST API ENDPOINTS =============

    //Tạo hoặc lấy chatroom
    @PostMapping("/api/chat/chatrooms")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrGetChatroom(@RequestBody ChatroomRequest request) {
        User currentUser = getCurrentUser();
        
        // Xác định buyer và seller
        Long buyerId = currentUser.getUserid();
        Long sellerId = request.getSellerId();
        
        Chatroom chatroom = chatService.getOrCreateChatroom(buyerId, sellerId, request.getOrderId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("chatroom", Map.of(
                "chatroomId", chatroom.getChatid(),
                "buyerId", chatroom.getBuyer().getUserid(),
                "buyerName", chatroom.getBuyer().getDisplayname() != null ? chatroom.getBuyer().getDisplayname() : chatroom.getBuyer().getUsername(),
                "sellerId", chatroom.getSeller().getUserid(),
                "sellerName", chatroom.getSeller().getDisplayname() != null ? chatroom.getSeller().getDisplayname() : chatroom.getSeller().getUsername(),
                "orderId", chatroom.getOrders() != null ? chatroom.getOrders().getOrderid() : null,
                "createdAt", chatroom.getCreatedat()
        ));
        
        return ResponseEntity.ok(response);
    }

    //Lấy tất cả chatrooms của user
    @GetMapping("/api/chat/chatrooms")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserChatrooms() {
        User currentUser = getCurrentUser();
        List<Chatroom> chatrooms = chatService.getChatroomsByUser(currentUser.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("chatrooms", chatrooms.stream().map(room -> {
            // Xác định đối tượng chat (buyer hay seller)
            boolean isUserBuyer = room.getBuyer().getUserid().equals(currentUser.getUserid());
            User otherUser = isUserBuyer ? room.getSeller() : room.getBuyer();
            
            int unreadCount = chatService.getUnreadMessageCount(room.getChatid(), currentUser.getUserid());
            
            return Map.of(
                    "chatroomId", room.getChatid(),
                    "otherUserId", otherUser.getUserid(),
                    "otherUserName", otherUser.getDisplayname() != null ? otherUser.getDisplayname() : otherUser.getUsername(),
                    "orderId", room.getOrders() != null ? room.getOrders().getOrderid() : null,
                    "unreadCount", unreadCount,
                    "createdAt", room.getCreatedat()
            );
        }).collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }

    //Lấy lịch sử chat của chatroom
    @GetMapping("/api/chat/chatrooms/{chatroomId}/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChatroomMessages(@PathVariable Long chatroomId) {
        User currentUser = getCurrentUser();
        List<Message> messages = chatService.getMessagesByChatroom(chatroomId);
        
        // Đánh dấu messages là đã đọc
        chatService.markMessagesAsRead(chatroomId, currentUser.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messages", messages.stream().map(msg -> Map.of(
                "messageId", msg.getMessid(),
                "senderId", msg.getSender().getUserid(),
                "senderName", msg.getSender().getDisplayname() != null ? msg.getSender().getDisplayname() : msg.getSender().getUsername(),
                "content", msg.getText(),
                "messageType", msg.getMessageType() != null ? msg.getMessageType() : "TEXT",
                "attachUrl", msg.getAttachUrl() != null ? msg.getAttachUrl() : "",
                "timestamp", msg.getCreatedat(),
                "isRead", msg.isRead()
        )).collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }

    // ============= WEBSOCKET HANDLERS =============

    /**
     * Handle gửi message
     * Client send to: /app/chat.sendMessage
     * Server broadcast to: /topic/chatroom/{chatroomId}
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO messageDTO) {
        // Lưu message vào DB
        Message savedMessage = chatService.saveMessage(messageDTO);
        
        // Prepare response
        ChatMessageDTO response = new ChatMessageDTO();
        response.setMessageId(savedMessage.getMessid());
        response.setChatroomId(savedMessage.getChatroom().getChatid());
        response.setSenderId(savedMessage.getSender().getUserid());
        response.setSenderName(savedMessage.getSender().getDisplayname() != null ? 
                savedMessage.getSender().getDisplayname() : savedMessage.getSender().getUsername());
        response.setContent(savedMessage.getText());
        response.setMessageType(savedMessage.getMessageType());
        response.setTimestamp(savedMessage.getCreatedat());
        response.setRead(false);
        
        // Broadcast message đến tất cả subscribers của chatroom này
        messagingTemplate.convertAndSend(
                "/topic/chatroom/" + savedMessage.getChatroom().getChatid(),
                response
        );
        
        // Gửi notification riêng cho receiver
        Chatroom chatroom = savedMessage.getChatroom();
        Long senderId = savedMessage.getSender().getUserid();
        String senderName = savedMessage.getSender().getDisplayname() != null ?
                savedMessage.getSender().getDisplayname() : savedMessage.getSender().getUsername();

        Long receiverId = senderId.equals(chatroom.getBuyer().getUserid())
                ? chatroom.getSeller().getUserid()
                : chatroom.getBuyer().getUserid();
        
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                response
        );

        // GUI NOTIFICATION CHO RECEIVER
        notificationService.notifyNewMessage(receiverId, senderId, senderName, chatroom.getChatid());
    }

    //Handle user join chatroom
    @MessageMapping("/chat.joinRoom")
    public void joinChatroom(@Payload Map<String, Object> payload) {
        Long chatroomId = Long.valueOf(payload.get("chatroomId").toString());
        Long userId = Long.valueOf(payload.get("userId").toString());
        
        // Đánh dấu tất cả messages trong room này là đã đọc
        chatService.markMessagesAsRead(chatroomId, userId);
        
        // Broadcast notification user joined (optional)
        Map<String, Object> joinNotification = new HashMap<>();
        joinNotification.put("type", "USER_JOINED");
        joinNotification.put("userId", userId);
        joinNotification.put("chatroomId", chatroomId);
        
        messagingTemplate.convertAndSend(
                "/topic/chatroom/" + chatroomId,
                joinNotification
        );
    }

    //Handle typing indicator
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        Long chatroomId = Long.valueOf(payload.get("chatroomId").toString());
        Long userId = Long.valueOf(payload.get("userId").toString());
        boolean isTyping = Boolean.parseBoolean(payload.get("isTyping").toString());
        
        Map<String, Object> typingNotification = new HashMap<>();
        typingNotification.put("type", "TYPING");
        typingNotification.put("userId", userId);
        typingNotification.put("isTyping", isTyping);
        
        messagingTemplate.convertAndSend(
                "/topic/chatroom/" + chatroomId,
                typingNotification
        );
    }

    // ============= HELPER METHODS =============
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
