package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.cart_items;

import java.util.List;

public interface CartService {
    void addToCart(long productId, long userid, int quantity) throws Exception;
    List<cart_items> viewCart(long userid);
    void updateCartItem(long productId, long userid, int quantity) throws Exception;
    void removeCartItem(long productId, long userid) throws Exception;
}