package com.project.tradingev_batter;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import com.project.tradingev_batter.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

import java.util.*;

@SpringBootApplication
@EnableScheduling
public class TradingevBatterApplication {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private PackageServiceRepository packageServiceRepository;

    @Autowired
    private ProductImgRepository productImgRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserPackageRepository userPackageRepository;

    public static void main(String[] args) {
        SpringApplication.run(TradingevBatterApplication.class, args);
    }

    @PostConstruct
    public void seedData() {
        System.out.println("========== STARTING DATA SEEDING ==========");

        // ============= SEED ROLES (Check if already exists) =============
        Role buyerRole = roleRepository.findByRolename("BUYER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRolename("BUYER");
                    role.setJoindate(new Date());
                    System.out.println("Created BUYER role");
                    return roleRepository.save(role);
                });

        Role sellerRole = roleRepository.findByRolename("SELLER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRolename("SELLER");
                    role.setJoindate(new Date());
                    System.out.println("Created SELLER role");
                    return roleRepository.save(role);
                });

        Role staffRole = roleRepository.findByRolename("STAFF")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRolename("STAFF");
                    role.setJoindate(new Date());
                    System.out.println("Created STAFF role");
                    return roleRepository.save(role);
                });

        Role managerRole = roleRepository.findByRolename("MANAGER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRolename("MANAGER");
                    role.setJoindate(new Date());
                    System.out.println("Created MANAGER role");
                    return roleRepository.save(role);
                });

        // ============= SEED USERS (Check if already exists) =============
        User buyer = userRepository.findByUsername("buyer");
        if (buyer == null) {
            buyer = new User();
            buyer.setUsername("buyer");
            buyer.setPassword(passwordEncoder.encode("buyer123"));
            buyer.setEmail("buyer@tradingev.com");
            buyer.setDisplayname("Nguyễn Văn A");
            buyer.setPhone("0901234567");
            buyer.setCreated_at(new Date());
            buyer.setIsactive(true);
            buyer.setRoles(Collections.singletonList(buyerRole));
            buyer = userRepository.save(buyer);
            System.out.println("Created BUYER user");
        }
        final User finalBuyer = buyer; // Make it final for lambda

        User seller = userRepository.findByUsername("seller");
        if (seller == null) {
            seller = new User();
            seller.setUsername("seller");
            seller.setPassword(passwordEncoder.encode("seller123"));
            seller.setEmail("seller@tradingev.com");
            seller.setDisplayname("Trần Thị B");
            seller.setPhone("0912345678");
            seller.setCreated_at(new Date());
            seller.setIsactive(true);
            seller.setSellerUpgradeStatus("APPROVED");
            seller.setRoles(Collections.singletonList(sellerRole));
            seller = userRepository.save(seller);
            System.out.println("Created SELLER user");
        }
        final User finalSeller = seller; // Make it final for lambda

        User staff = userRepository.findByUsername("staff");
        if (staff == null) {
            staff = new User();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setEmail("staff@tradingev.com");
            staff.setDisplayname("Nhân Viên Hệ Thống");
            staff.setPhone("0934567890");
            staff.setCreated_at(new Date());
            staff.setIsactive(true);
            staff.setRoles(Collections.singletonList(staffRole));
            staff = userRepository.save(staff);
            System.out.println("Created STAFF user");
        }
        final User finalStaff = staff; // Make it final for lambda

        User manager = userRepository.findByUsername("manager");
        if (manager == null) {
            manager = new User();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setEmail("manager@tradingev.com");
            manager.setDisplayname("Quản Trị Viên");
            manager.setPhone("0923456789");
            manager.setCreated_at(new Date());
            manager.setIsactive(true);
            manager.setRoles(Collections.singletonList(managerRole));
            manager = userRepository.save(manager);
            System.out.println("Created MANAGER user");
        }
        final User finalManager = manager; // Make it final for lambda

        // ============= SEED PACKAGE SERVICES (Check if already exists) =============
        PackageService carBasicPkg = packageServiceRepository.findByNameAndPackageType("Gói Cơ Bản Xe", "CAR")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói Cơ Bản Xe");
                    pkg.setPackageType("CAR");
                    pkg.setDurationMonths(1);
                    pkg.setPrice(100000.0);
                    pkg.setMaxCars(1);
                    pkg.setMaxBatteries(0);
                    pkg.setDescription("Đăng bán 1 xe trong 1 tháng");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Car Basic Package");
                    return packageServiceRepository.save(pkg);
                });

        packageServiceRepository.findByNameAndPackageType("Gói Chuyên Nghiệp Xe", "CAR")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói Chuyên Nghiệp Xe");
                    pkg.setPackageType("CAR");
                    pkg.setDurationMonths(6);
                    pkg.setPrice(500000.0);
                    pkg.setMaxCars(5);
                    pkg.setMaxBatteries(0);
                    pkg.setDescription("Đăng bán 5 xe trong 6 tháng");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Car Pro Package");
                    return packageServiceRepository.save(pkg);
                });

        packageServiceRepository.findByNameAndPackageType("Gói VIP Xe", "CAR")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói VIP Xe");
                    pkg.setPackageType("CAR");
                    pkg.setDurationMonths(12);
                    pkg.setPrice(900000.0);
                    pkg.setMaxCars(999);
                    pkg.setMaxBatteries(0);
                    pkg.setDescription("Đăng bán không giới hạn xe trong 1 năm");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Car VIP Package");
                    return packageServiceRepository.save(pkg);
                });

        PackageService batteryBasicPkg = packageServiceRepository.findByNameAndPackageType("Gói Cơ Bản Pin", "BATTERY")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói Cơ Bản Pin");
                    pkg.setPackageType("BATTERY");
                    pkg.setDurationMonths(1);
                    pkg.setPrice(50000.0);
                    pkg.setMaxCars(0);
                    pkg.setMaxBatteries(3);
                    pkg.setDescription("Đăng bán 3 pin trong 1 tháng");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Battery Basic Package");
                    return packageServiceRepository.save(pkg);
                });

        packageServiceRepository.findByNameAndPackageType("Gói Chuyên Nghiệp Pin", "BATTERY")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói Chuyên Nghiệp Pin");
                    pkg.setPackageType("BATTERY");
                    pkg.setDurationMonths(6);
                    pkg.setPrice(250000.0);
                    pkg.setMaxCars(0);
                    pkg.setMaxBatteries(15);
                    pkg.setDescription("Đăng bán 15 pin trong 6 tháng");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Battery Pro Package");
                    return packageServiceRepository.save(pkg);
                });

        packageServiceRepository.findByNameAndPackageType("Gói VIP Pin", "BATTERY")
                .orElseGet(() -> {
                    PackageService pkg = new PackageService();
                    pkg.setName("Gói VIP Pin");
                    pkg.setPackageType("BATTERY");
                    pkg.setDurationMonths(12);
                    pkg.setPrice(450000.0);
                    pkg.setMaxCars(0);
                    pkg.setMaxBatteries(999);
                    pkg.setDescription("Đăng bán không giới hạn pin trong 1 năm");
                    pkg.setCreatedAt(new Date());
                    System.out.println("Created Battery VIP Package");
                    return packageServiceRepository.save(pkg);
                });

        // ============= SEED USER PACKAGES FOR SELLER (Check if already exists) =============
        if (!userPackageRepository.existsByUserAndPackageService(finalSeller, carBasicPkg)) {
            UserPackage sellerCarPkg = new UserPackage();
            sellerCarPkg.setUser(finalSeller);
            sellerCarPkg.setPackageService(carBasicPkg);
            sellerCarPkg.setPurchaseDate(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            sellerCarPkg.setExpiryDate(cal.getTime());
            sellerCarPkg.setRemainingCars(1);
            sellerCarPkg.setRemainingBatteries(0);
            userPackageRepository.save(sellerCarPkg);
            System.out.println("Created UserPackage for SELLER (Car Package)");
        }

        if (!userPackageRepository.existsByUserAndPackageService(finalSeller, batteryBasicPkg)) {
            UserPackage sellerBatteryPkg = new UserPackage();
            sellerBatteryPkg.setUser(finalSeller);
            sellerBatteryPkg.setPackageService(batteryBasicPkg);
            sellerBatteryPkg.setPurchaseDate(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            sellerBatteryPkg.setExpiryDate(cal.getTime());
            sellerBatteryPkg.setRemainingCars(0);
            sellerBatteryPkg.setRemainingBatteries(3);
            userPackageRepository.save(sellerBatteryPkg);
            System.out.println("Created UserPackage for SELLER (Battery Package)");
        }

        // ============= SEED ADDRESS (Check if already exists) =============
        if (addressRepository.findByUsersAndStreet(finalBuyer, "123 Nguyễn Huệ").isEmpty()) {
            Address address = new Address();
            address.setStreet("123 Nguyễn Huệ");
            address.setWard("Phường Bến Nghé");
            address.setDistrict("Quận 1");
            address.setProvince("TP. Hồ Chí Minh");
            address.setCountry("Việt Nam");
            address.setUsers(finalBuyer);
            addressRepository.save(address);
            System.out.println("Created Address for BUYER");
        }

        // ============= SEED PRODUCTS (Check if already exists) =============
        Product carProduct = productRepository.findByProductnameAndUsers("Tesla Model 3 2020", finalSeller)
                .orElseGet(() -> {
                    // Create Brandcars first
                    Brandcars brandcar = new Brandcars();
                    brandcar.setYear(2020);
                    brandcar.setOdo(25000.0);
                    brandcar.setCapacity(75.0);
                    brandcar.setColor("Đỏ");

                    Product product = new Product();
                    product.setProductname("Tesla Model 3 2020");
                    product.setDescription("Xe điện Tesla Model 3, tình trạng tốt, đã qua sử dụng 2 năm");
                    product.setCost(800000000.0);
                    product.setAmount(1);
                    product.setStatus(ProductStatus.DANG_BAN);
                    product.setModel("Model 3");
                    product.setType("Car EV");
                    product.setSpecs("Công suất: 283hp, Tốc độ tối đa: 225km/h, Pin: 75kWh");
                    product.setCreatedat(new Date());
                    product.setUpdatedat(new Date());
                    product.setInWarehouse(true);
                    product.setViewCount(150);
                    product.setUsers(finalSeller);
                    product.setBrandcars(brandcar);
                    brandcar.setProducts(product);

                    product = productRepository.save(product);
                    System.out.println("Created Car Product: Tesla Model 3");
                    return product;
                });

        Product batteryProduct = productRepository.findByProductnameAndUsers("Pin LG Chem 60kWh", finalSeller)
                .orElseGet(() -> {
                    // Create Brandbattery first
                    Brandbattery brandbattery = new Brandbattery();
                    brandbattery.setYear(2021);
                    brandbattery.setCapacity(60.0);
                    brandbattery.setRemaining(85.0);

                    Product product = new Product();
                    product.setProductname("Pin LG Chem 60kWh");
                    product.setDescription("Pin lithium-ion chất lượng cao, còn 85% dung lượng");
                    product.setCost(50000000.0);
                    product.setAmount(3);
                    product.setStatus(ProductStatus.DANG_BAN);
                    product.setModel("LG Chem");
                    product.setType("Battery");
                    product.setSpecs("Dung lượng: 60kWh, Điện áp: 400V, Chu kỳ sạc: 2000+");
                    product.setCreatedat(new Date());
                    product.setUpdatedat(new Date());
                    product.setInWarehouse(false);
                    product.setViewCount(89);
                    product.setUsers(finalSeller);
                    product.setBrandbattery(brandbattery);
                    brandbattery.setProducts(product);

                    product = productRepository.save(product);
                    System.out.println("Created Battery Product: Pin LG Chem");
                    return product;
                });

        // ============= SEED PRODUCT IMAGES (Check if already exists) =============
        if (productImgRepository.findByProducts(carProduct).isEmpty()) {
            product_img img1 = new product_img();
            img1.setUrl("https://res.cloudinary.com/demo/image/upload/tesla_model3_front.jpg");
            img1.setProducts(carProduct);
            productImgRepository.save(img1);

            product_img img2 = new product_img();
            img2.setUrl("https://res.cloudinary.com/demo/image/upload/tesla_model3_side.jpg");
            img2.setProducts(carProduct);
            productImgRepository.save(img2);
            System.out.println("Created Product Images for Car");
        }

        if (productImgRepository.findByProducts(batteryProduct).isEmpty()) {
            product_img img = new product_img();
            img.setUrl("https://res.cloudinary.com/demo/image/upload/lg_chem_battery.jpg");
            img.setProducts(batteryProduct);
            productImgRepository.save(img);
            System.out.println("Created Product Images for Battery");
        }

        // ============= SEED POSTS (Check if already exists) =============
        if (!postRepository.existsByProducts(carProduct)) {
            Post carPost = new Post();
            carPost.setTitle("Bán xe Tesla Model 3 2020 - Giá tốt!");
            carPost.setDescription("Xe Tesla Model 3 màu đỏ, đã qua sử dụng 2 năm, tình trạng tốt");
            carPost.setStatus(PostStatus.DA_DUYET);
            carPost.setCreated_at(new Date());
            carPost.setUpdated_at(new Date());
            carPost.setProducts(carProduct);
            carPost.setUsers(finalSeller);
            postRepository.save(carPost);
            System.out.println("Created Post for Car Product");
        }

        if (!postRepository.existsByProducts(batteryProduct)) {
            Post batteryPost = new Post();
            batteryPost.setTitle("Bán pin LG Chem 60kWh - Còn 85% dung lượng");
            batteryPost.setDescription("Pin LG Chem chất lượng cao, phù hợp cho xe điện");
            batteryPost.setStatus(PostStatus.DA_DUYET);
            batteryPost.setCreated_at(new Date());
            batteryPost.setUpdated_at(new Date());
            batteryPost.setProducts(batteryProduct);
            batteryPost.setUsers(finalSeller);
            postRepository.save(batteryPost);
            System.out.println("Created Post for Battery Product");
        }

        // ============= SEED ORDERS (Check if already exists) =============
        if (orderRepository.findByUsersAndStatus(finalBuyer, OrderStatus.DA_HOAN_TAT).isEmpty()) {
            Orders completedOrder = new Orders();
            completedOrder.setTotalamount(50000000.0);
            completedOrder.setShippingfee(30000.0);
            completedOrder.setTotalfinal(50030000.0);
            completedOrder.setShippingaddress("123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM");
            completedOrder.setPaymentmethod("VnPay");
            completedOrder.setCreatedat(new Date());
            completedOrder.setUpdatedat(new Date());
            completedOrder.setStatus(OrderStatus.DA_HOAN_TAT);
            completedOrder.setUsers(finalBuyer);
            completedOrder = orderRepository.save(completedOrder);

            // Create order detail
            Order_detail orderDetail = new Order_detail();
            orderDetail.setQuantity(1);
            orderDetail.setUnit_price(50000000.0);
            orderDetail.setOrders(completedOrder);
            orderDetail.setProducts(batteryProduct);
            orderDetailRepository.save(orderDetail);

            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setAmount(50030000.0);
            transaction.setMethod("VnPay");
            transaction.setTransactionType(TransactionType.BATTERY_PAYMENT);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCreatedat(new Date());
            transaction.setOrders(completedOrder);
            transaction.setCreatedBy(finalBuyer);
            transactionRepository.save(transaction);

            System.out.println("Created Completed Order for BUYER");
        }

        // ============= SEED FEEDBACK (Check if already exists) =============
        if (feedbackRepository.findByProductsAndUsers(batteryProduct, finalBuyer).isEmpty()) {
            Feedback feedback = new Feedback();
            feedback.setRating(5);
            feedback.setComment("Pin chất lượng tốt, giao hàng nhanh!");
            feedback.setCreated_at(new Date());
            feedback.setProducts(batteryProduct);
            feedback.setUsers(finalBuyer);
            feedbackRepository.save(feedback);
            System.out.println("Created Feedback for Battery Product");
        }

        // ============= SEED NOTIFICATIONS (Check if already exists) =============
        if (notificationRepository.findByUsersAndTitle(finalManager, "Có sản phẩm mới cần duyệt").isEmpty()) {
            Notification notification = new Notification();
            notification.setTitle("Có sản phẩm mới cần duyệt");
            notification.setDescription("Seller đã đăng 1 sản phẩm xe mới cần kiểm duyệt");
            notification.setCreated_time(new Date());
            notification.setUsers(finalManager);
            notificationRepository.save(notification);
            System.out.println("Created Notification for MANAGER");
        }

        System.out.println("========== DATA SEEDING COMPLETED ==========");
    }
}
