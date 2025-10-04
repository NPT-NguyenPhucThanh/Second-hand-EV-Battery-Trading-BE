package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.cart_items;

@Repository
public interface CartItemRepository extends JpaRepository<cart_items, Long> {
}
