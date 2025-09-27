package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback,Long> {
}
