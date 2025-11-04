package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.ChatroomRepository;
import com.project.tradingev_batter.Repository.MessageRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.dto.ChatMessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatroomRepository chatroomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ChatServiceImpl(ChatroomRepository chatroomRepository,
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          OrderRepository orderRepository) {
        this.chatroomRepository = chatroomRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    //Lấy hoặc tạo chatroom giữa buyer và seller
    @Override
    @Transactional
    public Chatroom getOrCreateChatroom(Long buyerId, Long sellerId, Long orderId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Tìm chatroom đã tồn tại
        Chatroom existingRoom = null;
        if (orderId != null) {
            Orders order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                existingRoom = chatroomRepository.findByBuyerAndSellerAndOrders(buyer, seller, order);
            }
        } else {
            existingRoom = chatroomRepository.findByBuyerAndSeller(buyer, seller);
        }

        if (existingRoom != null) {
            return existingRoom;
        }

        // Tạo chatroom mới
        Chatroom newRoom = new Chatroom();
        newRoom.setBuyer(buyer);
        newRoom.setSeller(seller);
        newRoom.setCreatedat(new Date());

        if (orderId != null) {
            Orders order = orderRepository.findById(orderId).orElse(null);
            newRoom.setOrders(order);
        }

        return chatroomRepository.save(newRoom);
    }

    @Override
    public Chatroom getChatroomById(Long chatroomId) {
        return chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found"));
    }

    //Lấy tất cả chatroom của user (cả buyer và seller)
    @Override
    public List<Chatroom> getChatroomsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy chatroom mà user là buyer hoặc seller
        List<Chatroom> buyerRooms = chatroomRepository.findByBuyer(user);
        List<Chatroom> sellerRooms = chatroomRepository.findBySeller(user);

        // Merge 2 lists
        buyerRooms.addAll(sellerRooms);
        return buyerRooms;
    }

    //Lưu message vào DB
    @Override
    @Transactional
    public Message saveMessage(ChatMessageDTO messageDTO) {
        Chatroom chatroom = getChatroomById(messageDTO.getChatroomId());
        
        User sender = userRepository.findById(messageDTO.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        Message message = new Message();
        message.setChatroom(chatroom);
        message.setSender(sender);
        message.setText(messageDTO.getContent());
        message.setMessageType(messageDTO.getMessageType() != null ? messageDTO.getMessageType() : "TEXT");
        message.setRead(false);
        message.setCreatedat(new Date());
        
        // Set buyerid và sellerid để tương thích với schema cũ
        if (sender.getUserid().equals(chatroom.getBuyer().getUserid())) {
            message.setBuyerid(sender);
            message.setSellerid(chatroom.getSeller());
        } else {
            message.setBuyerid(chatroom.getBuyer());
            message.setSellerid(sender);
        }
        
        return messageRepository.save(message);
    }

    //Lấy tất cả messages trong chatroom
    @Override
    public List<Message> getMessagesByChatroom(Long chatroomId) {
        Chatroom chatroom = getChatroomById(chatroomId);
        return messageRepository.findByChatroomOrderByCreatedatAsc(chatroom);
    }

    //Đánh dấu tất cả messages là đã đọc
    @Override
    @Transactional
    public void markMessagesAsRead(Long chatroomId, Long userId) {
        Chatroom chatroom = getChatroomById(chatroomId);
        List<Message> unreadMessages = messageRepository.findByChatroomAndIsReadFalse(chatroom);
        
        for (Message msg : unreadMessages) {
            // Chỉ đánh dấu messages mà user không phải là người gửi
            if (!msg.getSender().getUserid().equals(userId)) {
                msg.setRead(true);
                messageRepository.save(msg);
            }
        }
    }

    //Đếm số messages chưa đọc
    @Override
    public int getUnreadMessageCount(Long chatroomId, Long userId) {
        return messageRepository.countUnreadMessages(chatroomId, userId);
    }

    // Lấy messages với pagination (20 messages/page, từ mới → cũ)
    @Override
    public Page<Message> getMessagesByChatroomPaginated(Long chatroomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChatroomIdOrderByCreatedatDesc(chatroomId, pageable);
    }
}
