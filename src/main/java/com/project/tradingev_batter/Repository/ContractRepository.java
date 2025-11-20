package com.project.tradingev_batter.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.tradingev_batter.Entity.Contracts;

@Repository
public interface ContractRepository extends JpaRepository<Contracts, Long> {

    // Tìm contract theo seller và status (chờ ký)
    List<Contracts> findBySellers_UseridAndStatusOrderBySignedatDesc(Long sellerId, boolean status);

    // Tìm contract theo order
    Optional<Contracts> findByOrders_Orderid(Long orderId);

    // Tìm contract theo docusealSubmissionId
    Optional<Contracts> findByDocusealSubmissionId(String docusealSubmissionId);

    // Tìm tất cả contract của seller
    List<Contracts> findBySellers_UseridOrderBySignedatDesc(Long sellerId);

    // Tìm tất cả contract của buyer
    List<Contracts> findByBuyers_UseridOrderBySignedatDesc(Long buyerId);

    // Tìm contract pending (chờ ký) của seller
    List<Contracts> findBySellers_UseridAndStatusFalseOrderBySignedatDesc(Long sellerId);
}
