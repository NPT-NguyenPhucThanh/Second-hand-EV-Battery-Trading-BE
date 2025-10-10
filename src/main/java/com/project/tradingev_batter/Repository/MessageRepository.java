package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatroomOrderByCreatedatAsc(Chatroom chatroom);
    List<Message> findByChatroomAndIsReadFalse(Chatroom chatroom);
}
