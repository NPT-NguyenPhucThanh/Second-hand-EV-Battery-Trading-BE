package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import com.project.tradingev_batter.dto.RefundRequest;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.DisputeStatus;
import com.project.tradingev_batter.enums.RefundStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ManagerServiceImpl implements ManagerService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;
    private final RoleRepository roleRepository;
    private final PackageServiceRepository packageServiceRepository;
    private final RefundRepository refundRepository;
    private final DocuSealService docuSealService;
    private final NotificationService notificationService;

    public ManagerServiceImpl(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              OrderRepository orderRepository,
                              DisputeRepository disputeRepository,
                              RoleRepository roleRepository,
                              PackageServiceRepository packageServiceRepository,
                              RefundRepository refundRepository,
                              DocuSealService docuSealService,
                              NotificationService notificationService) {
        this.refundRepository = refundRepository;
        this.packageServiceRepository = packageServiceRepository;
        this.docuSealService = docuSealService;
        this.roleRepository = roleRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.disputeRepository = disputeRepository;
        this.notificationService = notificationService;
    }

    //Sử dụng Notification entity. Khi seller đăng product (xe), tự động tạo notification cho managers.
    //ở đây giả định đã là manager r á
    @Override
    @Transactional
    public List<Notification> getNotiForManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        //thêm kiểm tra role ở đây nếu cần
        return notificationRepository.findByUsers(manager);
    }

    //Duyệt sơ bộ product (xe) của seller
    //nếu approved = true -> chuyển trạng thái product sang CHO_KIEM_DUYET
    //nếu approved = false -> chuyển trạng thái product sang BI_TU_CHOI
    //tạo noti cho seller với note
    @Override
    @Transactional
    public void approvePreliminaryProduct(Long productId, String note, boolean approved) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (approved) {
            product.setStatus(ProductStatus.CHO_KIEM_DUYET);
            // SU DUNG NOTIFICATIONSERVICE
            notificationService.notifyProductApproved(
                product.getUsers().getUserid(),
                productId,
                product.getProductname()
            );
        } else {
            product.setStatus(ProductStatus.BI_TU_CHOI);
            // SU DUNG NOTIFICATIONSERVICE
            notificationService.notifyProductRejected(
                product.getUsers().getUserid(),
                productId,
                product.getProductname(),
                note
            );
        }
        productRepository.save(product);
    }

    //Nhập kết quả kiểm định từ bên thứ 3
    //nếu passed = true -> chuyển trạng thái product sang DA_DUYET và tạo hợp đồng điện tử
    //nếu passed = false -> chuyển trạng thái product sang KHONG_DAT_KIEM_DINH
    //tạo noti cho seller với note
    @Override
    @Transactional
    public void inputInspectionResult(Long productId, boolean passed, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (passed) {
            product.setStatus(ProductStatus.DA_DUYET);

            // TAO HOP DONG DIEN TU QUA DOCUSEAL
            User seller = product.getUsers();
            User manager = getCurrentManager();
            
            try {
                // Goi DocuSealService de tao hop dong dang ban
                Contracts contract = docuSealService.createProductListingContract(product, seller, manager);
                
                log.info("Product listing contract created via DocuSeal. Contract ID: {}, Submission ID: {}",
                        contract.getContractid(), contract.getDocusealSubmissionId());
                
                // SU DUNG NOTIFICATIONSERVICE
                notificationService.notifyProductApproved(
                    seller.getUserid(),
                    productId,
                    product.getProductname()
                );

            } catch (Exception e) {
                log.error("Error creating DocuSeal contract for product: {}", productId, e);
                
                // SU DUNG NOTIFICATIONSERVICE cho loi
                notificationService.createNotification(
                    seller.getUserid(),
                    "Loi tao hop dong dien tu",
                    "Co loi xay ra khi tao hop dong dien tu. Vui long lien he ho tro. Loi: " + e.getMessage()
                );

                // Revert status
                product.setStatus(ProductStatus.CHO_KIEM_DUYET);
            }
            
        } else {
            product.setStatus(ProductStatus.KHONG_DAT_KIEM_DINH);
            // SU DUNG NOTIFICATIONSERVICE
            notificationService.notifyProductFailedInspection(
                product.getUsers().getUserid(),
                productId,
                product.getProductname()
            );
        }
        
        productRepository.save(product);
    }

    @Override
    @Transactional
    public List<Product> getWarehouseProducts() {
        return productRepository.findByTypeAndInWarehouse("Car EV", true); // Cần thêm custom query nếu chưa có
    }

    //chỉ có product type "Car EV" mới được thêm vào kho
    //khi thêm vào kho, set inWarehouse = true
    @Override
    @Transactional
    public void addToWarehouse(Long productId) {
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
    @Override
    @Transactional
    public void approveOrder(Long orderId, boolean approved, String note) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if(approved){
            order.setStatus(OrderStatus.DA_DUYET);
            // SU DUNG NOTIFICATIONSERVICE
            notificationService.notifyOrderApproved(
                order.getUsers().getUserid(),
                orderId,
                order.getAppointmentDate(),
                order.getTransactionLocation()
            );
        } else {
            order.setStatus(OrderStatus.BI_TU_CHOI);
            double depositeAmount = order.getTotalamount() * 0.1;
            processOrderRefundIfRejected(orderId, depositeAmount);

            // SU DUNG NOTIFICATIONSERVICE
            notificationService.notifyOrderRejected(
                order.getUsers().getUserid(),
                orderId,
                note
            );
        }
        orderRepository.save(order);
    }

    //Giải quyết tranh chấp
    //chuyển trạng thái tranh chấp sang RESOLVED, lưu resolution
    //cập nhật trạng thái đơn hàng sang DISPUTE_RESOLVED
    @Override
    @Transactional
        public void resolveDispute(Long disputeId, String resolution, RefundRequest refundRequest) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setManager(getCurrentManager()); // Giả sử có phương thức lấy manager hiện tại
        disputeRepository.save(dispute);

        //update order status
        Orders order = dispute.getOrder();
        order.setStatus(OrderStatus.DISPUTE_RESOLVED); // Or "RESOLVED_WITH_REFUND" nếu refund
        orderRepository.save(order);

        if (refundRequest != null && refundRequest.getAmount() > 0) {
            Refund refund = new Refund();
            refund.setAmount(refundRequest.getAmount());
            refund.setReason(refundRequest.getReason() != null ? refundRequest.getReason() : resolution);
            refund.setStatus(refundRequest.getStatus() != null ? RefundStatus.valueOf(refundRequest.getStatus()) : RefundStatus.PENDING);
            refund.setOrders(order);
            refundRepository.save(refund);  // Auto createdat via @CreationTimestamp

            order.setStatus(OrderStatus.RESOLVED_WITH_REFUND);
            orderRepository.save(order);  // Update lại order

            // Noti với refund info
            Notification refundNoti = new Notification();
            refundNoti.setTitle("Tranh chấp được giải quyết với hoàn tiền");
            refundNoti.setDescription("Số tiền hoàn: " + refundRequest.getAmount() + ". Lý do: " + resolution);
            refundNoti.setUsers(order.getUsers());  // Buyer
            notificationRepository.save(refundNoti);
        }

        //tạo noti cho buyer
        Notification buyerNoti = new Notification();
        buyerNoti.setTitle("Tranh chấp đã được giải quyết");
        buyerNoti.setDescription(resolution + (refundRequest != null ? " (Có hoàn tiền)" : ""));
        buyerNoti.setUsers(order.getUsers());
        notificationRepository.save(buyerNoti);

        //tạo noti cho seller
        User seller = order.getDetails().get(0).getProducts().getUsers();  // Lấy seller từ detail đầu
        Notification sellerNoti = new Notification();
        sellerNoti.setTitle("Tranh chấp đã được giải quyết");
        sellerNoti.setDescription(resolution);
        sellerNoti.setUsers(seller);
        notificationRepository.save(sellerNoti);
    }

    //Xử lý hoàn tiền đơn hàng bị từ chối (10% tiền cọc)
    //tạo refund với status "COMPLETED" (giả định auto-complete, integrate VnPay sau)
    @Override
    @Transactional
    public void processOrderRefundIfRejected(Long orderId, double depositAmount) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Refund refund = new Refund();
        refund.setAmount(depositAmount);
        refund.setReason("Từ chối đơn hàng - Hoàn cọc 10%");
        refund.setStatus(RefundStatus.COMPLETED);  // Giả định auto-complete, integrate VnPay sau
        refund.setOrders(order);
        refundRepository.save(refund);

        // Noti
        Notification noti = new Notification();
        noti.setTitle("Hoàn tiền cọc");
        noti.setDescription("Số tiền " + depositAmount + " đã được hoàn trả.");
        noti.setUsers(order.getUsers());
        notificationRepository.save(noti);
    }

    //Sản phẩm chờ nhập kho
    @Override
    @Transactional
    public List<Product> getPendingWarehouseProducts() {
        // Custom query: Products DA_DUYET nhưng !inWarehouse && type="Car EV"
        return productRepository.findByTypeAndInWarehouse("Car EV", false);  // Reuse existing, filter status="DA_DUYET" nếu cần add query
    }

    //Xóa sản phẩm khỏi kho
    //chỉ có product type "Car EV" mới được xóa khỏi kho
    //khi xóa khỏi kho, set inWarehouse = false và status = "REMOVED_FROM_WAREHOUSE"
    //tạo noti cho seller với reason
    @Override
    @Transactional
    public void removeFromWarehouse(Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!product.getInWarehouse()) {
            throw new RuntimeException("Product not in warehouse");
        }
        if (!"Car EV".equals(product.getType())) {
            throw new RuntimeException("Only Car EV products can be removed from warehouse");
        }
        product.setInWarehouse(false);
        product.setStatus(ProductStatus.REMOVED_FROM_WAREHOUSE);  // Update status
        productRepository.save(product);

        // Noti seller
        User seller = product.getUsers();
        Notification noti = new Notification();
        noti.setTitle("Sản phẩm đã được gỡ khỏi kho");
        noti.setDescription("Lý do: " + reason);
        noti.setUsers(seller);
        notificationRepository.save(noti);
    }

    //Cập nhật trạng thái kho - Chỉ chấp nhận các ProductStatus hợp lệ
    @Override
    @Transactional
    public void updateWarehouseStatus(Long productId, String newStatus) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!product.getInWarehouse()) {
            throw new RuntimeException("Product not in warehouse");
        }

        // Parse string to ProductStatus enum
        try {
            ProductStatus status = ProductStatus.valueOf(newStatus);
            product.setStatus(status);
            productRepository.save(product);

            // Noti nếu cần
            if (status == ProductStatus.DANG_BAN) {
                User seller = product.getUsers();
                Notification noti = new Notification();
                noti.setTitle("Sản phẩm đã được vận chuyển khỏi kho");
                noti.setUsers(seller);
                notificationRepository.save(noti);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid product status: " + newStatus);
        }
    }

    private User getCurrentManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("No authenticated manager");
        }
        String username = auth.getName();
        User manager = userRepository.findByUsername(username);
        if (manager == null) {
            throw new RuntimeException("Manager not found");
        }
        return manager;
    }

    //Duyệt nâng cấp tài khoản seller
    //nếu approved = true -> thêm role SELLER cho user và cập nhật status
    //nếu approved = false -> cập nhật status và lưu lý do từ chối
    @Override
    @Transactional
    public void approveSellerUpgrade(Long userId, boolean approved, String rejectionReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (approved) {
            // Thêm role SELLER cho user
            Role sellerRole = roleRepository.findByRolename("SELLER");
            if (sellerRole == null) {
                throw new RuntimeException("SELLER role not found");
            }
            user.getRoles().add(sellerRole);
            user.setSellerUpgradeStatus("APPROVED");
            user.setRejectionReason(null); // Clear rejection reason nếu có

            // Tạo notification cho user
            Notification notification = new Notification();
            notification.setTitle("Nâng cấp tài khoản thành công");
            notification.setDescription("Chúc mừng! Bạn đã trở thành Seller. Giờ bạn có thể đăng bán sản phẩm.");
            notification.setUsers(user);
            notificationRepository.save(notification);
        } else {
            // Từ chối yêu cầu
            user.setSellerUpgradeStatus("REJECTED");
            user.setRejectionReason(rejectionReason != null ? rejectionReason : "Không đủ điều kiện");

            // Tạo notification cho user
            Notification notification = new Notification();
            notification.setTitle("Yêu cầu nâng cấp bị từ chối");
            notification.setDescription("Lý do: " + user.getRejectionReason());
            notification.setUsers(user);
            notificationRepository.save(notification);
        }

        userRepository.save(user);
    }

    //Lấy danh sách yêu cầu nâng cấp seller đang chờ duyệt
    @Override
    @Transactional
    public List<User> getPendingSellerUpgradeRequests() {
        return userRepository.findBySellerUpgradeStatus("PENDING");
    }

    //Khóa/Mở khóa tài khoản người dùng
    //nếu lock = true -> set isActive = false
    //nếu lock = false -> set isActive = true
    @Override
    @Transactional
    public void lockUser(Long userId, boolean lock) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsactive(!lock);
        userRepository.save(user);

        //tạo noti cho user
        Notification notification = new Notification();
        notification.setTitle(lock ? "Tài khoản bị khóa" : "Tài khoản được mở khóa");
        notification.setDescription(lock ? "Tài khoản của bạn đã bị khóa do vi phạm chính sách." : "Tài khoản của bạn đã được mở khóa. Vui lòng tuân thủ chính sách của chúng tôi.");
        notification.setUsers(user);
        notificationRepository.save(notification);
    }

    //Quản lý gói dịch vụ
    @Override
    @Transactional
    public PackageService createPackage(PackageService pkg) {
        pkg.setCreatedAt(new Date());
        return packageServiceRepository.save(pkg);
    }

    //Cập nhật gói dịch vụ
    @Override
    @Transactional
    public PackageService updatePackage(Long id, PackageService pkg) {
        PackageService existingPkg = packageServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found"));
        existingPkg.setName(pkg.getName());
        existingPkg.setDurationMonths(pkg.getDurationMonths());
        existingPkg.setPrice(pkg.getPrice());
        //cần gì thì add thêm nha
        return packageServiceRepository.save(existingPkg);
    }

    //Báo cáo doanh thu
    //tính tổng doanh thu từ tất cả đơn hàng đã duyệt
    @Override
    @Transactional
    public Map<String, Object> getRevenueReport() {
        double totalSales = orderRepository.getTotalSales();
        double commission = totalSales * 0.05; //hoa hồng 5%
        Map<String,Object> rp = new HashMap<>();
        rp.put("totalSales", totalSales);
        rp.put("commission", commission);
        return rp;
    }

    @Override
    @Transactional
    public Map<String, Object> getSystemReport() {
        Map<String,Object> rp = new HashMap<>();
        rp.put("totalProduct", productRepository.count());
        rp.put("totalOrder", orderRepository.count());
        rp.put("totalUser", userRepository.count());
        rp.put("revenue", getRevenueReport().get("totalSales"));

        // Xu hướng: Sử dụng query group by createdat
        // Ví dụ: List<Object[]> trends = orderRepository.getOrdersByMonth();
        // thêm bên orderRepository rồi mà chưa làm ở đây
        return rp;
    }
}
