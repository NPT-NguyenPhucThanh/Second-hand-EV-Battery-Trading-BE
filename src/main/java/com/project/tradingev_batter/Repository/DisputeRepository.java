package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.tradingev_batter.Entity.Dispute;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}
