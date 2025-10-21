package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Feedback;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback,Long> {
    List<Feedback> findByProducts_Productid(Long productId);

    // Tìm feedback theo product và user (Seeds)
    Optional<Feedback> findByProductsAndUsers(Product product, User user);
}
