package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Carts;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.cart_items;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<cart_items, Long> {
    cart_items findByUsersAndProducts(User users, Product products);
    cart_items findByCartsAndProducts(Carts carts, Product products);
    List<cart_items> findByUsers(User users);
}
