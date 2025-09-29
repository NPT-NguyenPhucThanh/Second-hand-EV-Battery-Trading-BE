package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Dispute;

import java.util.List;

@Service
public class ManagerServiceImpl {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ContractsRepository contractsRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;

    public ManagerServiceImpl(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              ContractsRepository contractsRepository,
                              OrderRepository orderRepository,
                              DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.contractsRepository = contractsRepository;
    }

    //Sử dụng Notification entity. Khi seller đăng product (xe), tự động tạo notification cho managers.
    //ở đây giả định đã là manager r á
    public List<Notification> getNotiForManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        //thêm kiểm tra role ở đây nếu cần
        return notificationRepository.findByUserId(manager);
    }

    //Duyệt sơ bộ product (xe) của seller
    //nếu approved = true -> chuyển trạng thái product sang CHO_KIEM_DUYET
    //nếu approved = false -> chuyển trạng thái product sang BI_TU_CHOI
    //tạo noti cho seller với note
    public void approvePreliminaryProduct(Long productId, String note, boolean approved) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (approved) {
            product.setStatus(ProductStatus.CHO_KIEM_DUYET.toString());
        } else {
            product.setStatus(ProductStatus.BI_TU_CHOI.toString());
        }
        productRepository.save(product);

        //tạo noti cho seller với note
        User seller = product.getUsers();
        Notification notification = new Notification();
        notification.setTitle(approved ? "Xe được duyệt" : "Xe bị từ chối");
        notification.setDescription(note);
        notification.setUsers(seller);
        notificationRepository.save(notification);
    }

    //Nhập kết quả kiểm định từ bên thứ 3
    //nếu passed = true -> chuyển trạng thái product sang DA_DUYET và tạo hợp đồng điện tử
    //nếu passed = false -> chuyển trạng thái product sang KHONG_DAT_KIEM_DINH
    //tạo noti cho seller với note
    public void inputInspectionResult(Long productId, boolean passed, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (passed) {
            product.setStatus(ProductStatus.DA_DUYET.toString());
            //tạo hợp đồng điện tử
            //ở đây tui coi tutorial (sử dụng Contract entity)
            Contracts contracts = new Contracts();
            contracts.setOrders(null); //nếu ở đây không có liên kết order
            contracts.setBuyers(null);
            contracts.setSellers(product.getUsers());
            contracts.setAdmins(null); //đang chưa biết sao với đoạn này
            contracts.setStatus(true);
            contractsRepository.save(contracts);
        } else {
            product.setStatus(ProductStatus.KHONG_DAT_KIEM_DINH.toString());
        }
        productRepository.save(product); //noti cho seller với note
    }

    public List<Product> getWarehouseProducts() {
        return productRepository.findByTypeAndInWarehouse("Car EV", true); // Cần thêm custom query nếu chưa có
    }

    //chỉ có product type "Car EV" mới được thêm vào kho
    //khi thêm vào kho, set inWarehouse = true
    public void addWarehouseProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if(!"Car EV".equals(product.getType())) {
            throw new RuntimeException("Only Car EV products can be added to warehouse");
        }
        product.setInWarehouse(true);
        productRepository.save(product);
    }

    //Duyệt đơn hàng
    //nếu approved = true -> chuyển trạng thái đơn hàng sang DA_DUYET
    //nếu approved = false -> chuyển trạng thái đơn hàng sang BI_TU_CHOI
    public void approveOrder(Long orderId, boolean approved, String note) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(approved ? "DA_DUYET" : "BI_TU_CHOI");
        orderRepository.save(order);

        //tạo noti cho buyer
        Notification buyerNoti = new Notification();
        buyerNoti.setTitle(approved ? "Đơn hàng được duyệt" : "Đơn hàng bị từ chối");
        buyerNoti.setDescription(note);
        buyerNoti.setUsers(order.getUsers());
        notificationRepository.save(buyerNoti);

        //ở đây có thể gửi cho seller (từ product trong order_detail) nếu cần
    }


    public void resolveDispute(Long disputeId, String resolution) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        dispute.setStatus("RESOLVED");
        dispute.setResolution(resolution);
        //dispute.setResolvedBy(getCurrentManager()); // Giả sử có phương thức lấy manager hiện tại
        disputeRepository.save(dispute);

        //update order status
        Orders order = dispute.getOrder();
        order.setStatus("DISPUTE_RESOLVED");
        orderRepository.save(order);

        //tạo noti cho buyer và seller
        Notification buyerNoti = new Notification();
        buyerNoti.setTitle("Tranh chấp đã được giải quyết");
        buyerNoti.setDescription(resolution);
        buyerNoti.setUsers(order.getUsers());
        notificationRepository.save(buyerNoti);

        //tạo noti cho seller
        Notification sellerNoti = new Notification();
        sellerNoti.setTitle("Tranh chấp đã được giải quyết");
        sellerNoti.setDescription(resolution);
        sellerNoti.setUsers(order.getUsers());
        notificationRepository.save(sellerNoti);
    }

    private User getCurrentManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User manager = userRepository.findByUsername(username);
        // Check role MANAGER
        return manager;
    }
}
