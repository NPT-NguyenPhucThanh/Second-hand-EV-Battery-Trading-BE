package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.FeedbackRepository;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository, 
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              OrderRepository orderRepository,
                              NotificationRepository notificationRepository) {
        this.feedbackRepository = feedbackRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Feedback> getFeedbacksByProduct(Long productId) {
        return feedbackRepository.findByProducts_Productid(productId);
    }

    //Tính rating trung bình của seller dựa trên tất cả feedback của sản phẩm của seller đó
    @Override
    public double getAverageRatingBySeller(Long sellerId) {
        List<Product> sellerProducts = productRepository.findByUsers_Userid(sellerId);
        if (sellerProducts.isEmpty()) {
            return 0.0;
        }
        
        double totalRating = 0.0;
        int totalFeedbacks = 0;
        
        for (Product product : sellerProducts) {
            List<Feedback> feedbacks = feedbackRepository.findByProducts_Productid(product.getProductid());
            for (Feedback feedback : feedbacks) {
                totalRating += feedback.getRating();
                totalFeedbacks++;
            }
        }
        
        return totalFeedbacks > 0 ? totalRating / totalFeedbacks : 0.0;
    }

    //Đếm tổng số đánh giá của seller
    @Override
    public int getTotalReviewsBySeller(Long sellerId) {
        List<Product> sellerProducts = productRepository.findByUsers_Userid(sellerId);
        int total = 0;
        
        for (Product product : sellerProducts) {
            total += feedbackRepository.findByProducts_Productid(product.getProductid()).size();
        }
        
        return total;
    }

    //Tạo feedback mới (Buyer đánh giá sau khi hoàn tất giao dịch)
    @Override
    @Transactional
    public Feedback createFeedback(Feedback feedback) {
        feedback.setCreated_at(new Date());
        return feedbackRepository.save(feedback);
    }

    //Buyer tạo feedback sau khi hoàn tất đơn hàng
    @Override
    @Transactional
    public Feedback createFeedbackFromBuyer(Long buyerId, Long orderId, Long productId, 
                                           int rating, String comment) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getUsers().getUserid() != buyerId) {
            throw new RuntimeException("Bạn không có quyền đánh giá đơn hàng này");
        }
        
        // Kiểm tra đơn hàng đã hoàn tất chưa
        if (!"DA_HOAN_TAT".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể đánh giá sau khi đơn hàng hoàn tất");
        }
        
        // Kiểm tra đã đánh giá chưa
        List<Feedback> existingFeedbacks = feedbackRepository.findByProducts_Productid(productId);
        boolean alreadyReviewed = existingFeedbacks.stream()
                .anyMatch(f -> f.getUsers().getUserid() == buyerId &&
                              f.getOrders().getOrderid() == orderId);
        
        if (alreadyReviewed) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }
        
        // Tạo feedback
        Feedback feedback = new Feedback();
        feedback.setUsers(buyer);
        feedback.setOrders(order);
        feedback.setProducts(product);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCreated_at(new Date());
        feedback = feedbackRepository.save(feedback);
        
        // Tạo notification cho seller
        User seller = product.getUsers();
        createNotification(seller, "Nhận được đánh giá mới", 
                "Sản phẩm " + product.getProductname() + " nhận được đánh giá " + rating + " sao từ khách hàng.");
        
        return feedback;
    }

    //Seller xem tất cả feedback của mình
    @Override
    public List<Feedback> getFeedbacksForSeller(Long sellerId) {
        List<Product> sellerProducts = productRepository.findByUsers_Userid(sellerId);
        return sellerProducts.stream()
                .flatMap(product -> feedbackRepository.findByProducts_Productid(product.getProductid()).stream())
                .toList();
    }

    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }
}
