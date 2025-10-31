package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Repository.DisputeRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.enums.DisputeStatus;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.TransactionStatus;
import com.project.tradingev_batter.enums.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final RefundService refundService;
    private final TransactionRepository transactionRepository;

    public DisputeServiceImpl(DisputeRepository disputeRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository,
                             NotificationService notificationService,
                             RefundService refundService,
                             TransactionRepository transactionRepository) {
        this.disputeRepository = disputeRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Dispute createDispute(Long buyerId, Long orderId, String description, String reasonType) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra quyền sở hữu
        if (!order.getUsers().getUserid().equals(buyerId)) {
            throw new RuntimeException("Bạn không có quyền tạo khiếu nại cho đơn hàng này");
        }
        
        // Tạo dispute
        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setDescription(description);
        dispute.setStatus(DisputeStatus.OPEN); // Mở khiếu nại
        dispute.setCreatedAt(new Date());
        dispute = disputeRepository.save(dispute);
        
        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.TRANH_CHAP);
        order.setUpdatedat(new Date());
        orderRepository.save(order);
        
        // SỬ DỤNG NOTIFICATIONSERVICE cho buyer
        notificationService.createNotification(buyer.getUserid(),
            "Khiếu nại đã được gửi",
            "Khiếu nại của bạn cho đơn hàng #" + orderId + " đã được gửi. Manager sẽ xử lý trong thời gian sớm nhất.");

        // GỬI NOTIFICATION CHO SELLER
        if (order.getDetails() != null && !order.getDetails().isEmpty()) {
            Long sellerId = order.getDetails().get(0).getProducts().getUsers().getUserid();
            notificationService.notifyNewDispute(sellerId, orderId, description);
        }

        return dispute;
    }

    @Override
    public List<Dispute> getDisputesByBuyer(Long buyerId) {
        return disputeRepository.findByOrder_Users_Userid(buyerId);
    }

    @Override
    public List<Dispute> getAllDisputes() {
        return disputeRepository.findAll();
    }

    @Override
    public Dispute getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
    }

    @Override
    @Transactional
    public Dispute resolveDispute(Long disputeId, String decision, String managerNote) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new RuntimeException("Khiếu nại này đã được xử lý rồi");
        }

        Orders order = dispute.getOrder();
        Long buyerId = order.getUsers().getUserid();
        Long sellerId = order.getDetails().get(0).getProducts().getUsers().getUserid();

        if ("APPROVE_REFUND".equalsIgnoreCase(decision)) {
            // CHẤP NHẬN HOÀN TIỀN

            // 1. Tạo Refund request
            refundService.createRefund(
                order.getOrderid(),
                order.getTotalfinal(),
                "Khiếu nại được chấp nhận. Manager note: " + (managerNote != null ? managerNote : "N/A")
            );

            // 2. Release escrow về buyer (tạo transaction hoàn tiền)
            List<Transaction> escrowedTransactions = transactionRepository
                .findByOrders_Orderid(order.getOrderid())
                .stream()
                .filter(t -> t.getIsEscrowed() != null && t.getIsEscrowed())
                .toList();

            for (Transaction t : escrowedTransactions) {
                // Tạo transaction hoàn tiền
                Transaction refundTx = new Transaction();
                refundTx.setOrders(order);
                refundTx.setAmount(t.getAmount());
                refundTx.setTransactionType(TransactionType.REFUND);
                refundTx.setMethod(t.getMethod());
                refundTx.setStatus(TransactionStatus.SUCCESS);
                refundTx.setDescription("Hoàn tiền do khiếu nại #" + disputeId + " được chấp nhận");
                refundTx.setCreatedat(new Date());
                refundTx.setPaymentDate(new Date());
                refundTx.setIsEscrowed(false);
                transactionRepository.save(refundTx);

                // Đánh dấu transaction gốc không còn escrowed
                t.setIsEscrowed(false);
                transactionRepository.save(t);
            }

            // 3. Cập nhật order status
            order.setStatus(OrderStatus.DA_HUY);
            order.setUpdatedat(new Date());
            orderRepository.save(order);

            // 4. Cập nhật dispute status
            dispute.setStatus(DisputeStatus.RESOLVED);
            dispute.setResolvedAt(new Date());
            dispute.setDescription(dispute.getDescription() + " | Manager note: " + (managerNote != null ? managerNote : "Được chấp nhận"));
            disputeRepository.save(dispute);

            // 5. Gửi notification cho buyer & seller
            notificationService.createNotification(buyerId,
                "Khiếu nại được chấp nhận",
                "Khiếu nại #" + disputeId + " của bạn đã được chấp nhận. Số tiền " + order.getTotalfinal() + " VNĐ sẽ được hoàn lại.");

            notificationService.createNotification(sellerId,
                "Khiếu nại bị chấp nhận",
                "Khiếu nại #" + disputeId + " cho đơn hàng #" + order.getOrderid() + " đã được manager chấp nhận. Đơn hàng bị hủy.");

        } else if ("REJECT_DISPUTE".equalsIgnoreCase(decision)) {
            // TỪ CHỐI KHIẾU NẠI

            // 1. Cập nhật dispute status
            dispute.setStatus(DisputeStatus.CLOSED);
            dispute.setResolvedAt(new Date());
            dispute.setDescription(dispute.getDescription() + " | Manager note: " + (managerNote != null ? managerNote : "Không đủ căn cứ"));
            disputeRepository.save(dispute);

            // 2. Order giữ nguyên trạng thái hoặc quay về trạng thái trước đó
            // (Có thể thay đổi tùy business logic)
            if (order.getStatus() == OrderStatus.TRANH_CHAP) {
                // Quay về trạng thái trước đó (giả sử là DANG_VAN_CHUYEN hoặc CHO_XAC_NHAN)
                order.setStatus(OrderStatus.DANG_VAN_CHUYEN);
                order.setUpdatedat(new Date());
                orderRepository.save(order);
            }

            // 3. Gửi notification chỉ cho buyer
            notificationService.createNotification(buyerId,
                "Khiếu nại bị từ chối",
                "Khiếu nại #" + disputeId + " của bạn đã bị từ chối. Lý do: " + (managerNote != null ? managerNote : "Không đủ căn cứ để chấp nhận khiếu nại"));

        } else {
            throw new RuntimeException("Decision không hợp lệ. Chỉ chấp nhận: APPROVE_REFUND hoặc REJECT_DISPUTE");
        }

        return dispute;
    }
}
