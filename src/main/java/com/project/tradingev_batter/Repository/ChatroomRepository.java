package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Chatroom;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
    Chatroom findByBuyerAndSeller(User buyer, User seller);
    Chatroom findByBuyerAndSellerAndOrders(User buyer, User seller, Orders orders);
    List<Chatroom> findByBuyer(User buyer);
    List<Chatroom> findBySeller(User seller);
}
