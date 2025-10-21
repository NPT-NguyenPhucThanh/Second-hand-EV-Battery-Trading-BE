package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatroomOrderByCreatedatAsc(Chatroom chatroom);
    List<Message> findByChatroomAndIsReadFalse(Chatroom chatroom);

    // Pagination support: Load messages từ mới → cũ (DESC), 20 messages/page
    @Query("SELECT m FROM Message m WHERE m.chatroom.chatid = :chatroomId ORDER BY m.createdat DESC")
    Page<Message> findByChatroomIdOrderByCreatedatDesc(@Param("chatroomId") Long chatroomId, Pageable pageable);

    // Đếm unread messages
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatroom.chatid = :chatroomId AND m.isRead = false AND m.sender.userid != :userId")
    int countUnreadMessages(@Param("chatroomId") Long chatroomId, @Param("userId") Long userId);
}
