package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.DisputeRepository;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;

    public DisputeServiceImpl(DisputeRepository disputeRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository,
                             NotificationRepository notificationRepository) {
        this.disputeRepository = disputeRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public Dispute createDispute(Long buyerId, Long orderId, String description, String reasonType) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra quyền sở hữu
        if (order.getUsers().getUserid() != buyerId) {
            throw new RuntimeException("Bạn không có quyền tạo khiếu nại cho đơn hàng này");
        }
        
        // Tạo dispute
        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setDescription(description);
        dispute.setStatus("OPEN"); // Mở khiếu nại
        dispute.setCreatedAt(new Date());
        dispute = disputeRepository.save(dispute);
        
        // Cập nhật trạng thái đơn hàng
        order.setStatus("TRANH_CHAP");
        order.setUpdatedat(new Date());
        orderRepository.save(order);
        
        // Tạo notification cho buyer
        createNotification(buyer, "Khiếu nại đã được gửi", 
                "Khiếu nại của bạn cho đơn hàng #" + orderId + " đã được gửi. Manager sẽ xử lý trong thời gian sớm nhất.");
        
        // TODO: Tạo notification cho manager
        
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

    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }
}
