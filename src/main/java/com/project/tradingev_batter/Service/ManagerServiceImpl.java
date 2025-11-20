package com.project.tradingev_batter.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.tradingev_batter.Entity.Contracts;
import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.Refund;
import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.UserPackage;
import com.project.tradingev_batter.Repository.DisputeRepository;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.PackageServiceRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.Repository.RefundRepository;
import com.project.tradingev_batter.Repository.RoleRepository;
import com.project.tradingev_batter.Repository.UserPackageRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.enums.DisputeStatus;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.RefundStatus;

import lombok.extern.slf4j.Slf4j;

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
    private final UserPackageRepository userPackageRepository;

    public ManagerServiceImpl(NotificationRepository notificationRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            DisputeRepository disputeRepository,
            RoleRepository roleRepository,
            PackageServiceRepository packageServiceRepository,
            RefundRepository refundRepository,
            DocuSealService docuSealService,
            NotificationService notificationService,
            UserPackageRepository userPackageRepository) {
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
        this.userPackageRepository = userPackageRepository;
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

    // Lấy danh sách sản phẩm CHỜ DUYỆT (Staff cần duyệt sơ bộ)
    @Override
    @Transactional(readOnly = true)
    public List<Product> getPendingApprovalProducts() {
        return productRepository.findAll().stream()
                .filter(p -> ProductStatus.CHO_DUYET.equals(p.getStatus()))
                .toList();
    }

    // Lấy danh sách sản phẩm CHỜ KIỂM ĐỊNH (Đã duyệt sơ bộ, chờ bên thứ 3 kiểm định)
    @Override
    @Transactional(readOnly = true)
    public List<Product> getPendingInspectionProducts() {
        return productRepository.findAll().stream()
                .filter(p -> ProductStatus.CHO_KIEM_DUYET.equals(p.getStatus()))
                .toList();
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
    //khi thêm vào kho, set inWarehouse = true và status = DANG_BAN
    @Override
    @Transactional
    public void addToWarehouse(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!"Car EV".equals(product.getType())) {
            throw new RuntimeException("Only Car EV products can be added to warehouse");
        }
        product.setInWarehouse(true);
        product.setStatus(ProductStatus.DANG_BAN); // Set status to display on platform
        productRepository.save(product);
    }

    // Lấy tất cả orders
    @Override
    @Transactional(readOnly = true)
    public List<Orders> getAllOrders() {
        List<Orders> orders = orderRepository.findAll();
        // Force initialize lazy collections
        orders.forEach(order -> {
            if (order.getDetails() != null) {
                order.getDetails().forEach(detail -> {
                    if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                        detail.getProducts().getImgs().size();
                    }
                });
            }
        });
        return orders;
    }

    // Lấy orders theo status
    @Override
    @Transactional(readOnly = true)
    public List<Orders> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            List<Orders> orders = orderRepository.findAll().stream()
                    .filter(o -> orderStatus.equals(o.getStatus()))
                    .toList();
            // Force initialize lazy collections
            orders.forEach(order -> {
                if (order.getDetails() != null) {
                    order.getDetails().forEach(detail -> {
                        if (detail.getProducts() != null && detail.getProducts().getImgs() != null) {
                            detail.getProducts().getImgs().size();
                        }
                    });
                }
            });
            return orders;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    //Duyệt đơn hàng
    //nếu approved = true -> chuyển trạng thái đơn hàng sang DA_DUYET
    //nếu approved = false -> chuyển trạng thái đơn hàng sang BI_TU_CHOI
    @Override
    @Transactional
    public void approveOrder(Long orderId, boolean approved, String note) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (approved) {
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

    @Override
    public List<Dispute> getAllDisputes() {
        return disputeRepository.findAll();
    }

    // NOTE: resolveDispute đã được chuyển sang DisputeServiceImpl.resolveDispute()
    // ManagerController đang sử dụng DisputeService để xử lý dispute
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
        noti.setDescription("Số tiền " + depositAmount + " đã được hoàn tr��.");
        noti.setUsers(order.getUsers());
        notificationRepository.save(noti);
    }

    //Sản phẩm chờ nhập kho
    @Override
    @Transactional
    public List<Product> getPendingWarehouseProducts() {
        // Lấy danh sách xe đã đạt kiểm định (DA_DUYET) nhưng chưa vào kho
        return productRepository.findByTypeAndStatusAndInWarehouse("Car EV", ProductStatus.DA_DUYET, false);
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
            Role sellerRole = roleRepository.findByRolename("SELLER")
                    .orElseThrow(() -> new RuntimeException("SELLER role not found"));
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
        // Lay tat ca don hang va filter theo status
        List<Orders> completedOrders = orderRepository.findAll().stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .toList();

        // Tong doanh thu
        double totalRevenue = completedOrders.stream()
                .mapToDouble(Orders::getTotalamount)
                .sum();

        // Doanh thu tu xe
        double carRevenue = completedOrders.stream()
                .filter(o -> o.getDetails().stream()
                .anyMatch(d -> "Car EV".equals(d.getProducts().getType())))
                .mapToDouble(Orders::getTotalamount)
                .sum();

        // Doanh thu tu pin
        double batteryRevenue = completedOrders.stream()
                .filter(o -> o.getDetails().stream()
                .anyMatch(d -> "Battery".equals(d.getProducts().getType())))
                .mapToDouble(Orders::getTotalamount)
                .sum();

        // Hoa hong 5% tren tong doanh thu
        double totalCommission = totalRevenue * 0.05;

        // Doanh thu tu goi dich vu (Package Purchase)
        List<Orders> packageOrders = orderRepository.findAll().stream()
                .filter(o -> o.getPackageId() != null && OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .toList();

        double packageRevenue = packageOrders.stream()
                .mapToDouble(Orders::getTotalamount)
                .sum();

        // Tong doanh thu nen tang = Commission + Package Revenue
        double platformRevenue = totalCommission + packageRevenue;

        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", totalRevenue);
        report.put("carRevenue", carRevenue);
        report.put("batteryRevenue", batteryRevenue);
        report.put("totalCommission", totalCommission);
        report.put("commissionRate", "5%");
        report.put("packageRevenue", packageRevenue);
        report.put("platformRevenue", platformRevenue);
        report.put("totalCompletedOrders", completedOrders.size());
        report.put("totalPackagesSold", packageOrders.size());

        return report;
    }

    @Override
    @Transactional
    public Map<String, Object> getSystemReport() {
        // Thong ke tong quat
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        // Thong ke nguoi dung theo role
        long totalBuyers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                .anyMatch(r -> "BUYER".equals(r.getRolename())))
                .count();

        long totalSellers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                .anyMatch(r -> "SELLER".equals(r.getRolename())))
                .count();

        // Thong ke san pham
        long carsOnSale = productRepository.findAll().stream()
                .filter(p -> "Car EV".equals(p.getType()) && ProductStatus.DANG_BAN.equals(p.getStatus()))
                .count();

        long batteriesOnSale = productRepository.findAll().stream()
                .filter(p -> "Battery".equals(p.getType()) && ProductStatus.DANG_BAN.equals(p.getStatus()))
                .count();

        long pendingApprovalProducts = productRepository.findAll().stream()
                .filter(p -> ProductStatus.CHO_DUYET.equals(p.getStatus()))
                .count();

        long pendingInspectionProducts = productRepository.findAll().stream()
                .filter(p -> ProductStatus.CHO_KIEM_DUYET.equals(p.getStatus()))
                .count();

        long productsInWarehouse = productRepository.findAll().stream()
                .filter(Product::getInWarehouse)
                .count();

        // Thong ke don hang
        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> OrderStatus.CHO_DUYET.equals(o.getStatus()))
                .count();

        long completedOrders = orderRepository.findAll().stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .count();

        long disputeOrders = orderRepository.findAll().stream()
                .filter(o -> OrderStatus.TRANH_CHAP.equals(o.getStatus()))
                .count();

        // Thong ke tranh chap
        long totalDisputes = disputeRepository.count();
        long openDisputes = disputeRepository.findAll().stream()
                .filter(d -> DisputeStatus.OPEN.equals(d.getStatus())
                || DisputeStatus.IN_PROGRESS.equals(d.getStatus()))
                .count();

        // San pham xem nhieu nhat - Xu huong thi truong
        List<Product> allProducts = productRepository.findAll();
        Product mostViewedProduct = allProducts.stream()
                .max(Comparator.comparingInt(Product::getViewCount))
                .orElse(null);

        // Loai san pham pho bien nhat
        long totalCarViews = allProducts.stream()
                .filter(p -> "Car EV".equals(p.getType()))
                .mapToInt(Product::getViewCount)
                .sum();

        long totalBatteryViews = allProducts.stream()
                .filter(p -> "Battery".equals(p.getType()))
                .mapToInt(Product::getViewCount)
                .sum();

        String trendingCategory = totalCarViews > totalBatteryViews ? "Car EV" : "Battery";

        // Doanh thu
        Map<String, Object> revenueReport = getRevenueReport();

        Map<String, Object> report = new HashMap<>();

        // Thong tin nguoi dung
        report.put("totalUsers", totalUsers);
        report.put("totalBuyers", totalBuyers);
        report.put("totalSellers", totalSellers);

        // Thong tin san pham
        report.put("totalProducts", totalProducts);
        report.put("carsOnSale", carsOnSale);
        report.put("batteriesOnSale", batteriesOnSale);
        report.put("pendingApprovalProducts", pendingApprovalProducts);
        report.put("pendingInspectionProducts", pendingInspectionProducts);
        report.put("productsInWarehouse", productsInWarehouse);

        // Thong tin don hang
        report.put("totalOrders", totalOrders);
        report.put("pendingOrders", pendingOrders);
        report.put("completedOrders", completedOrders);
        report.put("disputeOrders", disputeOrders);

        // Thong tin tranh chap
        report.put("totalDisputes", totalDisputes);
        report.put("openDisputes", openDisputes);

        // Xu huong thi truong
        if (mostViewedProduct != null) {
            report.put("mostViewedProduct", Map.of(
                    "productId", mostViewedProduct.getProductid(),
                    "productName", mostViewedProduct.getProductname(),
                    "views", mostViewedProduct.getViewCount(),
                    "type", mostViewedProduct.getType()
            ));
        } else {
            report.put("mostViewedProduct", null);
        }

        report.put("trendingCategory", trendingCategory);
        report.put("totalCarViews", totalCarViews);
        report.put("totalBatteryViews", totalBatteryViews);

        // Doanh thu
        report.put("platformRevenue", revenueReport.get("platformRevenue"));
        report.put("totalRevenue", revenueReport.get("totalRevenue"));
        report.put("totalCommission", revenueReport.get("totalCommission"));

        return report;
    }

    // Lấy tất cả user đang sử dụng gói (còn hiệu lực)
    @Override
    @Transactional(readOnly = true)
    public List<UserPackage> getAllActiveUserPackages() {
        return userPackageRepository.findAllActivePackages(new Date());
    }

    // Lấy user đang sử dụng gói theo loại (CAR/BATTERY)
    @Override
    @Transactional(readOnly = true)
    public List<UserPackage> getActiveUserPackagesByType(String packageType) {
        // Validate packageType
        if (!"CAR".equals(packageType) && !"BATTERY".equals(packageType)) {
            throw new RuntimeException("Invalid package type. Must be 'CAR' or 'BATTERY'");
        }
        return userPackageRepository.findActivePackagesByType(packageType, new Date());
    }
}
