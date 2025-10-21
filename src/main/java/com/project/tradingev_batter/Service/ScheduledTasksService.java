package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Entity.UserPackage;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.Repository.UserPackageRepository;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

// 1. Release escrow sau 7 ngay (giai phong tien cho seller)
// 2. Cancel pending transactions qua han
// 3. Auto-confirm battery orders sau 3 ngay khong khieu nai
// 4. Auto-hide products khi het han goi

@Service
@Slf4j
public class ScheduledTasksService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserPackageRepository userPackageRepository;
    private final NotificationService notificationService;

    public ScheduledTasksService(TransactionRepository transactionRepository,
                                 OrderRepository orderRepository,
                                 ProductRepository productRepository,
                                 UserPackageRepository userPackageRepository,
                                 NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userPackageRepository = userPackageRepository;
        this.notificationService = notificationService;
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
                    Orders order = transaction.getOrders();
                    double totalAmount = transaction.getAmount();

                    // Tính hoa hồng 5%
                    double commission = totalAmount * 0.05;
                    double sellerReceives = totalAmount - commission;
                    
                    // Tạo transaction record cho commission
                    Transaction commissionTransaction = new Transaction();
                    commissionTransaction.setOrders(order);
                    commissionTransaction.setAmount(commission);
                    commissionTransaction.setTransactionType(com.project.tradingev_batter.enums.TransactionType.COMMISSION);
                    commissionTransaction.setStatus(com.project.tradingev_batter.enums.TransactionStatus.SUCCESS);
                    commissionTransaction.setMethod("PLATFORM_FEE");
                    commissionTransaction.setDescription("Hoa hồng 5% từ đơn hàng #" + order.getOrderid());
                    commissionTransaction.setCreatedat(new Date());
                    commissionTransaction.setPaymentDate(new Date());
                    transactionRepository.save(commissionTransaction);

                    // Giải phóng tiền
                    transaction.setIsEscrowed(false);
                    transaction.setStatus(com.project.tradingev_batter.enums.TransactionStatus.SUCCESS);
                    transactionRepository.save(transaction);

                    // Cập nhật order status thành DA_HOAN_TAT nếu chưa
                    if (!OrderStatus.DA_HOAN_TAT.equals(order.getStatus())) {
                        order.setStatus(OrderStatus.DA_HOAN_TAT);
                        order.setUpdatedat(new Date());
                        orderRepository.save(order);
                    }

                    log.info("Escrow released for transaction {}: Order #{}, Amount: {}, Commission: {}, Seller receives: {}",
                            transaction.getTransactionCode(),
                            order.getOrderid(),
                            totalAmount,
                            commission,
                            sellerReceives);
                    
                    // GUI NOTIFICATION CHO SELLER
                    Long sellerId = order.getDetails().get(0).getProducts().getUsers().getUserid();
                    notificationService.notifyEscrowReleased(sellerId, order.getOrderid(), totalAmount, commission);

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
                    
                    // GUI NOTIFICATION CHO USER
                    if (transaction.getCreatedBy() != null) {
                        notificationService.notifyTransactionCancelled(
                            transaction.getCreatedBy().getUserid(),
                            transaction.getTransactionCode()
                        );
                    }

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

    // AUTO-CONFIRM BATTERY ORDERS sau 3 ngay
    // Chay moi ngay luc 1 gio sang
    @Scheduled(cron = "0 0 1 * * ?") // 1:00 AM every day
    @Transactional
    public void autoConfirmBatteryOrders() {
        try {
            log.info("Starting auto-confirm battery orders job...");

            Date threeDaysAgo = new Date(System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000));

            List<Orders> ordersToConfirm = orderRepository.findAll().stream()
                    .filter(order -> {
                        boolean isBatteryOrder = order.getDetails() != null &&
                                order.getDetails().stream()
                                .anyMatch(detail -> "Battery".equalsIgnoreCase(detail.getProducts().getType()));

                        boolean isDelivered = OrderStatus.DA_GIAO.equals(order.getStatus());

                        boolean isOverThreeDays = order.getUpdatedat() != null &&
                                order.getUpdatedat().before(threeDaysAgo);

                        return isBatteryOrder && isDelivered && isOverThreeDays;
                    })
                    .toList();

            if (ordersToConfirm.isEmpty()) {
                log.info("No battery orders ready to auto-confirm");
                return;
            }

            log.info("Found {} battery orders ready to auto-confirm", ordersToConfirm.size());

            for (Orders order : ordersToConfirm) {
                try {
                    order.setStatus(OrderStatus.DA_HOAN_TAT);
                    order.setUpdatedat(new Date());
                    orderRepository.save(order);

                    log.info("Auto-confirmed battery order #{}", order.getOrderid());

                    // GUI NOTIFICATION CHO BUYER VA SELLER
                    Long buyerId = order.getUsers().getUserid();
                    Long sellerId = order.getDetails().get(0).getProducts().getUsers().getUserid();
                    notificationService.notifyBatteryOrderAutoConfirmed(buyerId, sellerId, order.getOrderid());

                } catch (Exception e) {
                    log.error("Error auto-confirming order {}", order.getOrderid(), e);
                }
            }

            log.info("Auto-confirm battery orders job completed. Confirmed: {} orders", ordersToConfirm.size());

        } catch (Exception e) {
            log.error("Error in auto-confirm battery orders job", e);
        }
    }

    // AUTO-HIDE PRODUCTS khi het han goi
    // Chay moi ngay luc 1g05 sang (sau khi auto-confirm battery orders)
    @Scheduled(cron = "0 5 1 * * ?") // 1:05 AM every day
    @Transactional
    public void autoHideExpiredProducts() {
        try {
            log.info("Starting auto-hide expired products job...");

            Date currentDate = new Date();

            List<UserPackage> expiredPackages = userPackageRepository.findAll().stream()
                    .filter(pkg -> pkg.getExpiryDate() != null && pkg.getExpiryDate().before(currentDate))
                    .toList();

            if (expiredPackages.isEmpty()) {
                log.info("No expired packages found");
                return;
            }

            log.info("Found {} expired packages", expiredPackages.size());

            int totalProductsHidden = 0;

            for (UserPackage userPackage : expiredPackages) {
                try {
                    Long userId = userPackage.getUser().getUserid();

                    List<Product> userProducts = productRepository.findByUsers_Userid(userId).stream()
                            .filter(product ->
                                ProductStatus.DANG_BAN.equals(product.getStatus()) ||
                                ProductStatus.DA_DUYET.equals(product.getStatus())
                            )
                            .toList();

                    if (userProducts.isEmpty()) {
                        continue;
                    }

                    int hiddenCount = 0;
                    for (Product product : userProducts) {
                        product.setStatus(ProductStatus.HET_HAN);
                        product.setUpdatedat(new Date());
                        productRepository.save(product);

                        log.info("Hidden product {} (User: {}) due to expired package",
                                product.getProductid(), userId);
                        hiddenCount++;
                        totalProductsHidden++;
                    }

                    // GUI NOTIFICATION CHO SELLER
                    if (hiddenCount > 0) {
                        notificationService.notifyPackageExpired(userId, hiddenCount);
                    }

                } catch (Exception e) {
                    log.error("Error hiding products for user package {}",
                            userPackage.getUserpackageid(), e);
                }
            }

            log.info("Auto-hide expired products job completed. Hidden: {} products", totalProductsHidden);

        } catch (Exception e) {
            log.error("Error in auto-hide expired products job", e);
        }
    }
}
