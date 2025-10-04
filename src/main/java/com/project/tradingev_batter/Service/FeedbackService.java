package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FeedbackService {
    List<Feedback> getFeedbacksByProduct(Long productId);
}