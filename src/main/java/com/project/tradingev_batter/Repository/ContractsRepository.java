package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Contracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContractsRepository extends JpaRepository<Contracts, Long> {
    Contracts findByDocusealSubmissionId(String docusealSubmissionId);
    List<Contracts> findByContractType(String contractType);
    List<Contracts> findBySellers_Userid(Long sellerId);
    List<Contracts> findByBuyers_Userid(Long buyerId);
    Contracts findByOrders_Orderid(Long orderId);
}
