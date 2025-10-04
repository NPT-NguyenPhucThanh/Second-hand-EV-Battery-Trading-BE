package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Feedback;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback,Long> {
    List<Feedback> findByProducts_Productid(Long productId);
    @Query("SELECT SUM(o.totalfinal) FROM Orders o WHERE o.status = 'DA_HOAN_TAT'")
    Double getTotalSales();
}
