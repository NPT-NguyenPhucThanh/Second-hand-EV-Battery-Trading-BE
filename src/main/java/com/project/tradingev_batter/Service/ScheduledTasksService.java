package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.enums.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

//1. Release escrow sau 7 ngày (giải phóng tiền cho seller)
//2. Cancel pending transactions quá hạn
//3. Auto-confirm battery orders sau 3 ngày không khiếu nại

@Service
@Slf4j
public class ScheduledTasksService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public ScheduledTasksService(TransactionRepository transactionRepository,
                                 OrderRepository orderRepository) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
    }


    //RELEASE ESCROW - Giải phóng tiền sau 7 ngày
    //Chạy mỗi ngày lúc 2 giờ sáng
    @Scheduled(cron = "0 0 2 * * ?") // 2:00 AM every day
    @Transactional
    public void releaseEscrowedTransactions() {
        try {
            log.info("Starting escrow release job...");
            
            Date currentDate = new Date();
            List<Transaction> readyToRelease = transactionRepository
                    .findEscrowedTransactionsReadyToRelease(currentDate);
            
            if (readyToRelease.isEmpty()) {
                log.info("No transactions ready to release escrow");
                return;
            }
            
            log.info("Found {} transactions ready to release escrow", readyToRelease.size());
            
            for (Transaction transaction : readyToRelease) {
                try {
                    // Giải phóng tiền
                    transaction.setIsEscrowed(false);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transactionRepository.save(transaction);
                    
                    // TODO: Tích hợp VNPay để chuyển tiền thật cho seller
                    // Hiện tại chỉ update status trong DB
                    
                    Orders order = transaction.getOrders();
                    double totalAmount = transaction.getAmount();
                    double commission = totalAmount * 0.05; // 5% hoa hồng
                    double sellerReceives = totalAmount - commission;
                    
                    log.info("Escrow released for transaction {}: Order #{}, Amount: {}, Commission: {}, Seller receives: {}",
                            transaction.getTransactionCode(),
                            order.getOrderid(),
                            totalAmount,
                            commission,
                            sellerReceives);
                    
                    // TODO: Tạo notification cho seller
                    
                } catch (Exception e) {
                    log.error("Error releasing escrow for transaction {}", 
                            transaction.getTransactionCode(), e);
                }
            }
            
            log.info("Escrow release job completed. Released: {} transactions", readyToRelease.size());
            
        } catch (Exception e) {
            log.error("Error in escrow release job", e);
        }
    }

    //CANCEL EXPIRED PENDING TRANSACTIONS
    //Chạy mỗi giờ
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    @Transactional
    public void cancelExpiredPendingTransactions() {
        try {
            log.info("Starting expired pending transactions cleanup...");
            
            // Tìm transaction pending quá 2 giờ (VNPay timeout 1 giờ + buffer 1 giờ)
            Date expiredDate = new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000));
            List<Transaction> expiredTransactions = transactionRepository
                    .findExpiredPendingTransactions(expiredDate);
            
            if (expiredTransactions.isEmpty()) {
                log.info("No expired pending transactions found");
                return;
            }
            
            log.info("Found {} expired pending transactions", expiredTransactions.size());
            
            for (Transaction transaction : expiredTransactions) {
                try {
                    transaction.setStatus(TransactionStatus.CANCELLED);
                    transactionRepository.save(transaction);
                    
                    log.info("Cancelled expired transaction: {}", transaction.getTransactionCode());
                    
                    // TODO: Tạo notification cho user
                    
                } catch (Exception e) {
                    log.error("Error cancelling transaction {}", 
                            transaction.getTransactionCode(), e);
                }
            }
            
            log.info("Expired transactions cleanup completed. Cancelled: {}", expiredTransactions.size());
            
        } catch (Exception e) {
            log.error("Error in expired transactions cleanup", e);
        }
    }

    // TODO: Thêm scheduled task cho auto-confirm battery orders sau 3 ngày
}
