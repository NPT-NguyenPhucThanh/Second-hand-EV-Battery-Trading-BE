package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemsRepository;

    @Override
    public void addToCart(long productId, long userid, int quantity) throws Exception {
        User user = userRepository.findByUserid(userid);
        Product product = productRepository.findProductByProductid(productId);

        if (user == null) throw new Exception("User not found");
        if (product == null) throw new Exception("Product not found");

        Carts cart = user.getCarts();
        if (cart == null) throw new Exception("Cart not found for user");

        cart_items existingItem = cartItemsRepository.findByCartsAndProducts(cart, product);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setAddedat(new Date());
            cartItemsRepository.save(existingItem);
        } else {
            cart_items newItem = new cart_items();
            newItem.setUsers(user);
            newItem.setProducts(product);
            newItem.setQuantity(quantity);
            newItem.setAddedat(new Date());
            newItem.setCarts(cart);
            cartItemsRepository.save(newItem);
        }
    }

    @Override
    public List<cart_items> viewCart(long userid) {
        User user = userRepository.findByUserid(userid);
        if (user == null || user.getCarts() == null) {
            throw new RuntimeException("User or Cart not found");
        }
        return user.getCarts().getCart_items();
    }

    @Override
    public void updateCartItem(long productId, long userid, int quantity) throws Exception {
        User user = userRepository.findByUserid(userid);
        if (user == null) throw new Exception("User not found");

        Carts cart = user.getCarts();
        if (cart == null) throw new Exception("Cart not found");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        cart_items cartItem = cartItemsRepository.findByCartsAndProducts(cart, product);
        if (cartItem == null) throw new Exception("Cart item not found");

        if (quantity <= 0) {
            cartItemsRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemsRepository.save(cartItem);
        }
    }

    @Override
    public void removeCartItem(long productId, long userid) throws Exception {
        User user = userRepository.findByUserid(userid);
        if (user == null) throw new Exception("User not found");

        Carts cart = user.getCarts();
        if (cart == null) throw new Exception("Cart not found");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        cart_items cartItem = cartItemsRepository.findByCartsAndProducts(cart, product);
        if (cartItem == null) throw new Exception("Cart item not found");

        cartItemsRepository.delete(cartItem);
    }
}