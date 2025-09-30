package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Dispute;
import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagerServiceImpl {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ContractsRepository contractsRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;
    private final RoleRepository roleRepository;
    private final PackageServiceRepository packageServiceRepository;

    public ManagerServiceImpl(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              ContractsRepository contractsRepository,
                              OrderRepository orderRepository,
                              DisputeRepository disputeRepository,
                              RoleRepository roleRepository,
                              PackageServiceRepository packageServiceRepository) {
        this.packageServiceRepository = packageServiceRepository;
        this.roleRepository = roleRepository;
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
            contracts.setSignedat(new Date());
            contracts.setOrders(null); //nếu ở đây không có liên kết order
            contracts.setBuyers(null);
            contracts.setSellers(product.getUsers());
            contracts.setAdmins(getCurrentManager()); //đang chưa biết sao với đoạn này
            contracts.setStatus(true);
            contractsRepository.save(contracts);

            //tạo noti cho seller
            Notification sellerNotif = new Notification();
            sellerNotif.setTitle("Xe đạt kiểm định");
            sellerNotif.setDescription("Vui lòng ký hợp đồng điện tử.");
            sellerNotif.setUsers(product.getUsers());
            notificationRepository.save(sellerNotif);
        } else {
            product.setStatus(ProductStatus.KHONG_DAT_KIEM_DINH.toString());
        }
        productRepository.save(product);
        // Tạo noti chung
        Notification noteNotif = new Notification();
        noteNotif.setTitle(passed ? "Kiểm định thành công" : "Kiểm định thất bại");
        noteNotif.setDescription(note);
        noteNotif.setUsers(product.getUsers());
        notificationRepository.save(noteNotif);
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
        if (!order.getDetails().isEmpty()) {
            User seller = order.getDetails().get(0).getProducts().getUsers();
            Notification sellerNotif = new Notification();
            sellerNotif.setTitle(approved ? "Đơn hàng được duyệt" : "Đơn hàng bị từ chối");
            sellerNotif.setDescription(note);
            sellerNotif.setUsers(seller);
            notificationRepository.save(sellerNotif);
        }
    }

    //Giải quyết tranh chấp
    //chuyển trạng thái tranh chấp sang RESOLVED, lưu resolution
    //cập nhật trạng thái đơn hàng sang DISPUTE_RESOLVED
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

        //tạo noti cho buyer
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
    //nếu approved = true -> thêm role SELLER cho user
    public void approveSellerUpgrade(Long sellerId, boolean approved) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if(approved) {
            Role sellerRole = roleRepository.findByRoleName("SELLER");
            if(sellerRole == null) {
                throw new RuntimeException("SELLER role not found");
            }
            seller.getRoles().add(sellerRole);
        }
        userRepository.save(seller);

        //tạo noti cho seller
        Notification notification = new Notification();
        notification.setTitle(approved ? "Nâng cấp tài khoản thành công" : "Nâng cấp tài khoản thất bại");
        notification.setDescription(approved ? "Bạn giờ là seller." : "Yêu cầu bị từ chối.");
        notification.setUsers(seller);
        notificationRepository.save(notification);
    }

    //Khóa/Mở khóa tài khoản người dùng
    //nếu lock = true -> set isActive = false
    //nếu lock = false -> set isActive = true
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
    public PackageService createPackage(PackageService pkg) {
        pkg.setCreatedAt(new Date());
        return packageServiceRepository.save(pkg);
    }

    //Cập nhật gói dịch vụ
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
    public Map<String, Object> getRevenueReport() {
        double totalSales = orderRepository.getTotalSales();
        double commission = totalSales * 0.05; //hoa hồng 5%
        Map<String,Object> rp = new HashMap<>();
        rp.put("totalSales", totalSales);
        rp.put("commission", commission);
        return rp;
    }

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
