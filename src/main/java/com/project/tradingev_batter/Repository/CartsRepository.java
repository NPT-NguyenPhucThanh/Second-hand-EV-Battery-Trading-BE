package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Carts;

@Repository
public interface CartsRepository extends JpaRepository<Carts, Long> {
    Carts findByUsers(User user);
}
