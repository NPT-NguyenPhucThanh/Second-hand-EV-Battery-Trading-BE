package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Tao notification cho mot user
    @Transactional
    public Notification createNotification(Long userId, String title, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Notification notification = new Notification();
        notification.setUsers(user);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", userId, title);

        return saved;
    }

    // Lay tat ca notifications cua user
    public List<Notification> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return notificationRepository.findByUsers(user);
    }

    // Xoa notification
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("Deleted notification {}", notificationId);
    }

    // === HELPER METHODS - Tao notifications cho cac su kien cu the ===================================================

    // Notification khi dat coc thanh cong
    public void notifyDepositSuccess(Long buyerId, Long orderId, double amount) {
        createNotification(
            buyerId,
            "Dat coc thanh cong",
            String.format("Ban da dat coc thanh cong %.0f VND cho don hang #%d. Vui long cho Manager duyet.", amount, orderId)
        );
    }

    // Notification khi Manager duyet don hang
    public void notifyOrderApproved(Long buyerId, Long orderId, Date appointmentDate, String location) {
        createNotification(
            buyerId,
            "Don hang da duoc duyet",
            String.format("Don hang #%d da duoc duyet. Thoi gian giao dich: %s tai %s",
                orderId, appointmentDate != null ? appointmentDate.toString() : "Chua xac dinh", location)
        );
    }

    // Notification khi Manager tu choi don hang
    public void notifyOrderRejected(Long buyerId, Long orderId, String reason) {
        createNotification(
            buyerId,
            "Don hang bi tu choi",
            String.format("Don hang #%d da bi tu choi. Ly do: %s. Tien coc se duoc hoan lai.", orderId, reason)
        );
    }

    // Notification khi escrow release (seller nhan tien)
    public void notifyEscrowReleased(Long sellerId, Long orderId, double amount, double commission) {
        double sellerReceives = amount - commission;
        createNotification(
            sellerId,
            "Nhan tien thanh cong",
            String.format("Don hang #%d da hoan tat. Ban nhan duoc %.0f VND (sau khi tru hoa hong %.0f VND).",
                orderId, sellerReceives, commission)
        );
    }

    // Notification khi san pham duoc duyet
    public void notifyProductApproved(Long sellerId, Long productId, String productName) {
        createNotification(
            sellerId,
            "San pham da duoc duyet",
            String.format("San pham '%s' (ID: %d) da duoc duyet va dang hien thi tren nen tang.", productName, productId)
        );
    }

    // Notification khi san pham bi tu choi
    public void notifyProductRejected(Long sellerId, Long productId, String productName, String reason) {
        createNotification(
            sellerId,
            "San pham bi tu choi",
            String.format("San pham '%s' (ID: %d) da bi tu choi. Ly do: %s", productName, productId, reason)
        );
    }

    // Notification khi san pham khong dat kiem dinh
    public void notifyProductFailedInspection(Long sellerId, Long productId, String productName) {
        createNotification(
            sellerId,
            "San pham khong dat kiem dinh",
            String.format("San pham '%s' (ID: %d) khong dat kiem dinh. Vui long kiem tra lai.", productName, productId)
        );
    }

    // Notification khi goi het han
    public void notifyPackageExpired(Long sellerId, int hiddenProductsCount) {
        createNotification(
            sellerId,
            "Goi dich vu da het han",
            String.format("Goi dich vu cua ban da het han. %d san pham da bi an. Vui long gia han de tiep tuc ban hang.",
                hiddenProductsCount)
        );
    }

    // Notification khi don pin tu dong xac nhan
    public void notifyBatteryOrderAutoConfirmed(Long buyerId, Long sellerId, Long orderId) {
        createNotification(
            buyerId,
            "Don hang da hoan tat",
            String.format("Don hang pin #%d da duoc xac nhan tu dong sau 3 ngay. Cam on ban da mua hang!", orderId)
        );

        createNotification(
            sellerId,
            "Don hang da hoan tat",
            String.format("Don hang pin #%d da duoc xac nhan tu dong. Tien se duoc chuyen cho ban sau khi tru hoa hong.", orderId)
        );
    }

    // Notification khi co message moi
    public void notifyNewMessage(Long receiverId, Long senderId, String senderName, Long chatroomId) {
        createNotification(
            receiverId,
            "Ban co tin nhan moi",
            String.format("%s da gui tin nhan cho ban. Xem tai chatroom #%d", senderName, chatroomId)
        );
    }

    // Notification khi seller upgrade duoc duyet
    public void notifySellerUpgradeApproved(Long userId) {
        createNotification(
            userId,
            "Nang cap Seller thanh cong",
            "Chuc mung! Tai khoan cua ban da duoc nang cap len Seller. Ban co the bat dau dang ban san pham."
        );
    }

    // Notification khi seller upgrade bi tu choi
    public void notifySellerUpgradeRejected(Long userId, String reason) {
        createNotification(
            userId,
            "Nang cap Seller bi tu choi",
            String.format("Yeu cau nang cap Seller cua ban da bi tu choi. Ly do: %s", reason)
        );
    }

    // Notification khi co dispute moi
    public void notifyNewDispute(Long sellerId, Long orderId, String description) {
        createNotification(
            sellerId,
            "Co khieu nai moi",
            String.format("Don hang #%d co khieu nai: %s. Manager se xu ly som.", orderId, description)
        );
    }

    // Notification khi dispute duoc giai quyet
    public void notifyDisputeResolved(Long buyerId, Long orderId, String resolution) {
        createNotification(
            buyerId,
            "Khieu nai da duoc giai quyet",
            String.format("Khieu nai cua ban cho don hang #%d da duoc giai quyet: %s", orderId, resolution)
        );
    }

    // Notification khi transaction bi huy
    public void notifyTransactionCancelled(Long userId, String transactionCode) {
        createNotification(
            userId,
            "Giao dich bi huy",
            String.format("Giao dich %s da bi huy do het han thanh toan.", transactionCode)
        );
    }
}
