package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Carts;
import com.project.tradingev_batter.Entity.cart_items;

public interface CartService {
    // Thêm vào giỏ hàng
    cart_items addToCart(Long userId, Long productId, int quantity);
    
    // Xem giỏ hàng
    Carts getCart(Long userId);
    
    // Xóa khỏi giỏ hàng
    void removeFromCart(Long userId, Long itemId);
    
    // Cập nhật số lượng
    cart_items updateCartItemQuantity(Long userId, Long itemId, int quantity);
    
    // Tính tổng tiền giỏ hàng
    double calculateCartTotal(Long userId);
    
    // Xóa toàn bộ giỏ hàng sau khi checkout
    void clearCart(Long userId);
}
