package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Tìm transaction theo transaction code
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    // Tìm transaction theo VNPay transaction number
    Optional<Transaction> findByVnpayTransactionNo(String vnpayTransactionNo);
    
    // Lấy tất cả transaction của một order
    List<Transaction> findByOrders_Orderid(Long orderId);
    
    // Lấy tất cả transaction của một user
    List<Transaction> findByCreatedBy_Userid(Long userId);
    
    // Tìm các transaction đang escrow (giữ tiền 7 ngày)
    // EAGER LOAD orders, details, products, users để tránh LazyInitializationException
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN FETCH t.orders o " +
           "LEFT JOIN FETCH o.details d " +
           "LEFT JOIN FETCH d.products p " +
           "LEFT JOIN FETCH p.users " +
           "WHERE t.isEscrowed = true AND t.status = 'SUCCESS' AND t.escrowReleaseDate <= :currentDate")
    List<Transaction> findEscrowedTransactionsReadyToRelease(@Param("currentDate") Date currentDate);
    
    // Tìm transaction theo status
    List<Transaction> findByStatus(TransactionStatus status);
    
    // Tìm transaction pending quá lâu (để auto-cancel)
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdat < :expiredDate")
    List<Transaction> findExpiredPendingTransactions(@Param("expiredDate") Date expiredDate);
}
