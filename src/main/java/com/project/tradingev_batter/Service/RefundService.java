package com.project.tradingev_batter.Service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Refund;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.RefundRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.RefundStatus;
import com.project.tradingev_batter.enums.TransactionStatus;
import com.project.tradingev_batter.enums.TransactionType;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public RefundService(RefundRepository refundRepository,
            OrderRepository orderRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    //Tạo yêu cầu refund (gọi từ Buyer hoặc Manager)
    public Refund createRefund(Long orderId, double amount, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));

        Refund refund = new Refund();
        refund.setOrders(order);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);
        refund.setCreatedat(new Date());

        return refundRepository.save(refund);
    }

    //Manager xử lý refund
    //approve true = chấp nhận, false = từ chối
    //note Ghi chú (nếu từ chối)
    @Transactional
    public Refund processRefund(Long refundId, Long managerId, String refundMethod, boolean approve, String note) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund request không tồn tại"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new RuntimeException("Refund đã được xử lý rồi");
        }

        Orders order = refund.getOrders();

        if (approve) {
            // CHẤP NHẬN REFUND
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setRefundMethod(refundMethod);
            refund.setProcessedAt(new Date());

            // Lưu manager xử lý (cần load từ UserRepository)
            User manager = new User();
            manager.setUserid(managerId);
            refund.setProcessedBy(manager);

            // Tạo transaction record cho refund
            Transaction refundTransaction = new Transaction();
            refundTransaction.setOrders(order);
            refundTransaction.setAmount(refund.getAmount());
            refundTransaction.setTransactionType(TransactionType.REFUND);
            refundTransaction.setMethod(refundMethod);
            refundTransaction.setStatus(TransactionStatus.SUCCESS);
            refundTransaction.setCreatedat(new Date());
            refundTransaction.setPaymentDate(new Date());
            refundTransaction.setDescription("Hoàn tiền cho đơn hàng #" + order.getOrderid() + " - Lý do: " + refund.getReason());
            transactionRepository.save(refundTransaction);

            // Cập nhật order status
            order.setStatus(OrderStatus.RESOLVED_WITH_REFUND);
            orderRepository.save(order);

            // Gửi notification cho buyer
            notificationService.createNotification(
                    order.getUsers().getUserid(),
                    "Yêu cầu hoàn tiền được chấp nhận",
                    "Yêu cầu hoàn tiền cho đơn hàng #" + order.getOrderid() + " đã được chấp nhận. Số tiền " + refund.getAmount() + " VNĐ sẽ được hoàn lại qua " + refundMethod + "."
            );

        } else {
            // TỪ CHỐI REFUND
            refund.setStatus(RefundStatus.REJECTED);
            refund.setProcessedAt(new Date());
            refund.setReason(refund.getReason() + " | Manager note: " + (note != null ? note : "Không đủ căn cứ để hoàn tiền"));

            User manager = new User();
            manager.setUserid(managerId);
            refund.setProcessedBy(manager);

            // Gửi notification cho buyer
            notificationService.createNotification(
                    order.getUsers().getUserid(),
                    "Yêu cầu hoàn tiền bị từ chối",
                    "Yêu cầu hoàn tiền cho đơn hàng #" + order.getOrderid() + " đã bị từ chối. Lý do: " + (note != null ? note : "Không đủ căn cứ để hoàn tiền")
            );
        }

        return refundRepository.save(refund);
    }

    //Lấy tất cả refund requests (cho Manager)
    @Transactional(readOnly = true)
    public List<Refund> getAllRefunds() {
        List<Refund> refunds = refundRepository.findAll();
        // Force initialize lazy collections
        refunds.forEach(refund -> {
            if (refund.getOrders() != null && refund.getOrders().getDetails() != null) {
                refund.getOrders().getDetails().forEach(detail -> {
                    if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                        detail.getProducts().getImgs().size();
                    }
                });
            }
        });
        return refunds;
    }

    //Lấy refund requests theo status
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByStatus(RefundStatus status) {
        List<Refund> refunds = refundRepository.findAll().stream()
                .filter(refund -> refund.getStatus() == status)
                .toList();
        // Force initialize lazy collections
        refunds.forEach(refund -> {
            if (refund.getOrders() != null && refund.getOrders().getDetails() != null) {
                refund.getOrders().getDetails().forEach(detail -> {
                    if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                        detail.getProducts().getImgs().size();
                    }
                });
            }
        });
        return refunds;
    }

    //Lấy refunds của một order
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        List<Refund> refunds = refundRepository.findByOrders(order);
        // Force initialize lazy collections
        refunds.forEach(refund -> {
            if (refund.getOrders() != null && refund.getOrders().getDetails() != null) {
                refund.getOrders().getDetails().forEach(detail -> {
                    if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                        detail.getProducts().getImgs().size();
                    }
                });
            }
        });
        return refunds;
    }

    //Lấy refunds của buyer
    public List<Refund> getRefundsByBuyer(Long buyerId) {
        return refundRepository.findAll().stream()
                .filter(refund -> refund.getOrders().getUsers().getUserid().equals(buyerId))
                .toList();
    }

    //Lấy refund detail theo ID
    @Transactional(readOnly = true)
    public Refund getRefundById(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund không tồn tại"));
        // Force initialize lazy collections
        if (refund.getOrders() != null && refund.getOrders().getDetails() != null) {
            refund.getOrders().getDetails().forEach(detail -> {
                if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                    detail.getProducts().getImgs().size();
                }
            });
        }
        return refund;
    }
}
