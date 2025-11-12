package com.project.tradingev_batter.Service;

import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.tradingev_batter.Entity.Carts;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.cart_items;
import com.project.tradingev_batter.Repository.CartItemRepository;
import com.project.tradingev_batter.Repository.CartsRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.enums.ProductStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CartServiceImpl implements CartService {

    private final CartsRepository cartsRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CartServiceImpl(CartsRepository cartsRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.cartsRepository = cartsRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public cart_items addToCart(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // XE KHÔNG THỂ THÊM VÀO GIỎ HÀNG
        if ("Car EV".equals(product.getType())) {
            throw new RuntimeException("Xe điện không thể thêm vào giỏ hàng. Vui lòng mua ngay!");
        }

        // Kiểm tra sản phẩm có đang bán không
        // Chỉ cho phép thêm vào giỏ nếu product status là DANG_BAN hoặc DA_DUYET
        if (!ProductStatus.DANG_BAN.equals(product.getStatus())
                && !ProductStatus.DA_DUYET.equals(product.getStatus())) {
            throw new RuntimeException("Sản phẩm không khả dụng để mua. Status hiện tại: " + product.getStatus());
        }

        // Kiểm tra còn hàng không (cho pin)
        if ("Battery".equals(product.getType()) && product.getAmount() < quantity) {
            throw new RuntimeException("Sản phẩm không đủ số lượng. Còn lại: " + product.getAmount());
        }

        // Lấy hoặc tạo cart
        Carts cart = cartsRepository.findByUsers(user);
        if (cart == null) {
            cart = new Carts();
            cart.setUsers(user);
            cart.setCreatedat(new Date());
            cart = cartsRepository.save(cart);
        }

        // Kiểm tra xem sản phẩm đã có trong giỏ chưa
        Optional<cart_items> existingItem = cart.getCart_items().stream()
                .filter(item -> item.getProducts().getProductid() == productId)
                .findFirst();

        if (existingItem.isPresent()) {
            // Cập nhật số lượng
            cart_items item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // Check lại số lượng nếu là battery
            if ("Battery".equals(product.getType()) && product.getAmount() < newQuantity) {
                throw new RuntimeException("Sản phẩm không đủ số lượng. Còn lại: " + product.getAmount());
            }

            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        } else {
            // Thêm mới
            cart_items newItem = new cart_items();
            newItem.setCarts(cart);
            newItem.setProducts(product);
            newItem.setUsers(user);
            newItem.setQuantity(quantity);
            newItem.setAddedat(new Date());
            return cartItemRepository.save(newItem);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Carts getCart(Long userId) {
        // Clear cache trước khi query để đảm bảo lấy dữ liệu mới nhất từ DB
        entityManager.clear();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Carts cart = cartsRepository.findByUsers(user);
        if (cart == null) {
            // Tạo cart mới nếu chưa có
            cart = new Carts();
            cart.setUsers(user);
            cart.setCreatedat(new Date());
            cart = cartsRepository.save(cart);
        }

        // Force initialize lazy collections
        cart.getCart_items().size();

        return cart;
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        cart_items item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Kiểm tra quyền sở hữu
        if (!item.getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa item này");
        }

        // Lấy cart trước khi xóa
        Carts cart = item.getCarts();

        // Remove item từ collection trước
        cart.getCart_items().remove(item);

        // Xóa item
        cartItemRepository.delete(item);
        cartItemRepository.flush();

        // Update cart timestamp
        cart.setUpdatedat(new Date());
        cartsRepository.save(cart);

        // Force flush để đảm bảo SQL DELETE được thực thi ngay
        entityManager.flush();

        // Clear cache để đảm bảo lần query tiếp theo
        entityManager.clear();
    }

    @Override
    @Transactional
    public cart_items updateCartItemQuantity(Long userId, Long itemId, int quantity) {
        cart_items item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Kiểm tra quyền sở hữu
        if (!item.getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật item này");
        }

        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateCartTotal(Long userId) {
        Carts cart = getCart(userId);

        return cart.getCart_items().stream()
                .mapToDouble(item -> item.getProducts().getCost() * item.getQuantity())
                .sum();
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Carts cart = getCart(userId);

        // Clear the collection FIRST before deleting from DB
        cart.getCart_items().clear();
        cart.setUpdatedat(new Date());

        // Save cart with empty items collection
        cartsRepository.save(cart);

        // Force flush to persist changes
        entityManager.flush();
        entityManager.clear();
    }
}
