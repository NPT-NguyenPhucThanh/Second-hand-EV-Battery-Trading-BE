package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Order_detail;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<Order_detail,Long> {
    List<Order_detail> findByOrders(Orders orders);
}
