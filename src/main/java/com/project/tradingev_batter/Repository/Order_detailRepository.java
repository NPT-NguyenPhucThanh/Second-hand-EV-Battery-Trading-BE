package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Order_detail;

@Repository
public interface Order_detailRepository extends JpaRepository<Order_detail,Long> {
}
