package com.project.tradingev_batter.Service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    public RefundService(RefundRepository refundRepository,
                        OrderRepository orderRepository,
                        TransactionRepository transactionRepository) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
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

        } else {
            // TỪ CHỐI REFUND
            refund.setStatus(RefundStatus.REJECTED);
            refund.setProcessedAt(new Date());
            refund.setReason(refund.getReason() + " | Manager note: " + (note != null ? note : "Không đủ căn cứ để hoàn tiền"));

            User manager = new User();
            manager.setUserid(managerId);
            refund.setProcessedBy(manager);
        }

        return refundRepository.save(refund);
    }

    //Lấy tất cả refund requests (cho Manager)
    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }

    //Lấy refund requests theo status
    public List<Refund> getRefundsByStatus(RefundStatus status) {
        return refundRepository.findAll().stream()
                .filter(refund -> refund.getStatus() == status)
                .toList();
    }

    //Lấy refunds của một order
    public List<Refund> getRefundsByOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        return refundRepository.findByOrders(order);
    }

    //Lấy refunds của buyer
    public List<Refund> getRefundsByBuyer(Long buyerId) {
        return refundRepository.findAll().stream()
                .filter(refund -> refund.getOrders().getUsers().getUserid().equals(buyerId))
                .toList();
    }

    //Lấy refund detail theo ID
    public Refund getRefundById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund không tồn tại"));
    }
}
