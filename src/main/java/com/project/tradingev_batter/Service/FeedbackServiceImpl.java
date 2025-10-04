package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public List<Feedback> getFeedbacksByProduct(Long productId) {
        return feedbackRepository.findByProducts_Productid(productId);  // Add method in repository
    }
}