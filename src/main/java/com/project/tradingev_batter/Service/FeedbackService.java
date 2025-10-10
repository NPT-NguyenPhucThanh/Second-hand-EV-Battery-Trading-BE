package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Feedback;

import java.util.List;

public interface FeedbackService {
    List<Feedback> getFeedbacksByProduct(Long productId);
    
    // Hỗ trợ Guest xem rating của seller
    double getAverageRatingBySeller(Long sellerId);
    int getTotalReviewsBySeller(Long sellerId);
    
    // Buyer tạo feedback
    Feedback createFeedback(Feedback feedback);
    Feedback createFeedbackFromBuyer(Long buyerId, Long orderId, Long productId, int rating, String comment);
    
    // Seller xem feedback của mình
    List<Feedback> getFeedbacksForSeller(Long sellerId);
}
