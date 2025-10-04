package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Refund;
import com.project.tradingev_batter.Entity.Orders;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund,Long> {
    List<Refund> findByOrders(Orders order); //check refund của order
}
