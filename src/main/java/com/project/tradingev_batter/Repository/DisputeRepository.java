package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByOrder_Users_Userid(Long userId);
}
