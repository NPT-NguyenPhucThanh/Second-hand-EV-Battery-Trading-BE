package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SellerServiceImpl implements SellerService {

    private final UserRepository userRepository;
    private final PackageServiceRepository packageServiceRepository;
    private final UserPackageRepository userPackageRepository;
    private final ProductRepository productRepository;
    private final BrandcarsRepository brandcarsRepository;
    private final BrandBatteryRepository brandBatteryRepository;
    private final ProductImgRepository productImgRepository;
    private final OrderRepository orderRepository;
    private final ImageUploadService imageUploadService;
    private final NotificationService notificationService;

    public SellerServiceImpl(UserRepository userRepository,
                            PackageServiceRepository packageServiceRepository,
                            UserPackageRepository userPackageRepository,
                            ProductRepository productRepository,
                            BrandcarsRepository brandcarsRepository,
                            BrandBatteryRepository brandBatteryRepository,
                            ProductImgRepository productImgRepository,
                            OrderRepository orderRepository,
                            ImageUploadService imageUploadService,
                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.packageServiceRepository = packageServiceRepository;
        this.userPackageRepository = userPackageRepository;
        this.productRepository = productRepository;
        this.brandcarsRepository = brandcarsRepository;
        this.brandBatteryRepository = brandBatteryRepository;
        this.productImgRepository = productImgRepository;
        this.orderRepository = orderRepository;
        this.imageUploadService = imageUploadService;
        this.notificationService = notificationService;
    }

    //TẠO ORDER MUA GÓI - Tạo order mua gói (chưa active UserPackage)
    //UserPackage chỉ được active sau khi thanh toán thành công
    @Override
    @Transactional
    public Map<String, Object> createPackagePurchaseOrder(Long sellerId, Long packageId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        PackageService packageService = packageServiceRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));
        
        // VALIDATE - Phai mua dung loai goi
        // Nếu gói XE thì maxBatteries phải = 0
        // Nếu gói PIN thì maxCars phải = 0
        if ("CAR".equals(packageService.getPackageType()) && packageService.getMaxBatteries() > 0) {
            throw new RuntimeException("Goi xe khong duoc co maxBatteries");
        }
        if ("BATTERY".equals(packageService.getPackageType()) && packageService.getMaxCars() > 0) {
            throw new RuntimeException("Goi pin khong duoc co maxCars");
        }
        
        // Tao order mua goi
        Orders order = new Orders();
        order.setTotalamount(packageService.getPrice());
        order.setShippingfee(0);
        order.setTotalfinal(packageService.getPrice());
        order.setShippingaddress("N/A - Package Purchase");
        order.setPaymentmethod("VNPAY");
        order.setCreatedat(new Date());
        order.setStatus(com.project.tradingev_batter.enums.OrderStatus.CHO_THANH_TOAN);
        order.setUsers(seller);
        order.setPackageId(packageId);
        order = orderRepository.save(order);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderid());
        result.put("packageId", packageId);
        result.put("packageName", packageService.getName());
        result.put("packagePrice", packageService.getPrice());
        result.put("packageType", packageService.getPackageType());
        
        // SU DUNG NOTIFICATIONSERVICE thay vi tao truc tiep
        notificationService.createNotification(seller.getUserid(),
            "Don hang mua goi da tao",
            "Vui long thanh toan don hang #" + order.getOrderid() + " de kich hoat goi " + packageService.getName());

        return result;
    }

    //ACTIVATE USER PACKAGE - Kích hoạt gói sau khi thanh toán thành công
    //Method này sẽ được gọi từ PaymentController sau khi VNPay callback success
    @Override
    @Transactional
    public UserPackage activatePackageAfterPayment(Long sellerId, Long packageId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        PackageService packageService = packageServiceRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));
        
        // Tao UserPackage moi
        UserPackage userPackage = new UserPackage();
        userPackage.setUser(seller);
        userPackage.setPackageService(packageService);
        userPackage.setPurchaseDate(new Date());
        
        // Tinh expiry date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, packageService.getDurationMonths());
        userPackage.setExpiryDate(calendar.getTime());
        
        // Set so luot dang
        userPackage.setRemainingCars(packageService.getMaxCars());
        userPackage.setRemainingBatteries(packageService.getMaxBatteries());
        
        userPackageRepository.save(userPackage);
        
        // BR-31: VALIDATE - Kiem tra lai loai goi
        validatePackageType(packageService);
        
        // SU DUNG NOTIFICATIONSERVICE
        notificationService.createNotification(seller.getUserid(),
            "Kich hoat goi thanh cong",
            "Goi " + packageService.getName() + " da duoc kich hoat. Han su dung den " + userPackage.getExpiryDate());

        return userPackage;
    }

    //Lấy gói hiện tại của seller
    @Override
    public UserPackage getCurrentPackage(Long sellerId) {
        List<UserPackage> packages = userPackageRepository.findByUser_UseridOrderByExpiryDateDesc(sellerId);
        
        // Lấy gói chưa hết hạn
        for (UserPackage pkg : packages) {
            if (!isPackageExpired(pkg)) {
                return pkg;
            }
        }
        
        return null; // Không có gói hợp lệ
    }

    @Override
    public boolean isPackageExpired(UserPackage userPackage) {
        return userPackage.getExpiryDate().before(new Date());
    }

    //Gia hạn gói
    @Override
    @Transactional
    public UserPackage renewPackage(Long sellerId, Long packageId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        PackageService packageService = packageServiceRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));
        
        UserPackage currentPackage = getCurrentPackage(sellerId);
        
        if (currentPackage != null && !isPackageExpired(currentPackage)) {
            // Gia han tu ngay het han hien tai
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentPackage.getExpiryDate());
            calendar.add(Calendar.MONTH, packageService.getDurationMonths());
            currentPackage.setExpiryDate(calendar.getTime());
            
            // Cong them luot dang
            currentPackage.setRemainingCars(currentPackage.getRemainingCars() + packageService.getMaxCars());
            currentPackage.setRemainingBatteries(currentPackage.getRemainingBatteries() + packageService.getMaxBatteries());
            
            userPackageRepository.save(currentPackage);
            
            // SU DUNG NOTIFICATIONSERVICE
            notificationService.createNotification(seller.getUserid(),
                "Gia han goi thanh cong",
                "Goi cua ban da duoc gia han den " + currentPackage.getExpiryDate());

            return currentPackage;
        } else {
            // Mua goi moi
            return activatePackageAfterPayment(sellerId, packageId);
        }
    }

    //KIỂM TRA XEM SELLER CÓ GÓI XE HỢP LỆ KHÔNG
    //Seller phải có gói packageType = "CAR" còn lượt đăng
    @Override
    public boolean canPostCar(Long sellerId) {
        UserPackage currentCarPackage = getCurrentPackageByType(sellerId, "CAR");
        return currentCarPackage != null && 
               !isPackageExpired(currentCarPackage) && 
               currentCarPackage.getRemainingCars() > 0;
    }


    //KIỂM TRA XEM SELLER CÓ GÓI PIN HỢP LỆ KHÔNG
    //Seller phải có gói packageType = "BATTERY" còn lượt đăng
    @Override
    public boolean canPostBattery(Long sellerId) {
        UserPackage currentBatteryPackage = getCurrentPackageByType(sellerId, "BATTERY");
        return currentBatteryPackage != null && 
               !isPackageExpired(currentBatteryPackage) && 
               currentBatteryPackage.getRemainingBatteries() > 0;
    }
    
    //LẤY GÓI HIỆN TẠI THEO LOẠI (CAR hoặc BATTERY)
    private UserPackage getCurrentPackageByType(Long sellerId, String packageType) {
        List<UserPackage> packages = userPackageRepository.findByUser_UseridOrderByExpiryDateDesc(sellerId);
        
        for (UserPackage pkg : packages) {
            if (!isPackageExpired(pkg) && packageType.equals(pkg.getPackageService().getPackageType())) {
                return pkg;
            }
        }
        
        return null;
    }

    //Đăng xe (cần kiểm định và hợp đồng)
    @Override
    @Transactional
    public Product createCarProduct(Long sellerId, String productname, String description, double cost,
                                   String licensePlate, String model, String specs, String brand, int year,
                                   MultipartFile[] images) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        // Tạo product
        Product product = new Product();
        product.setProductname(productname);
        product.setDescription(description);
        product.setCost(cost);
        product.setAmount(1); // Xe chỉ có 1 chiếc
        product.setStatus(ProductStatus.CHO_DUYET); // Chờ duyệt sơ bộ
        product.setModel(model);
        product.setType("Car EV");
        product.setSpecs(specs);
        product.setCreatedat(new Date());
        product.setInWarehouse(false);
        product.setUsers(seller);
        
        product = productRepository.save(product);
        
        // Tạo Brandcars
        Brandcars brandcars = new Brandcars();
        brandcars.setBrand(brand);
        brandcars.setYear(year);
        brandcars.setLicensePlate(licensePlate);
        brandcars.setProducts(product);
        brandcarsRepository.save(brandcars);
        
        // Upload images
        if (images != null && images.length > 0) {
            uploadImages(product, images);
        }
        
        //Giảm lượt đăng từ gói XE
        UserPackage currentCarPackage = getCurrentPackageByType(sellerId, "CAR");
        if (currentCarPackage == null) {
            throw new RuntimeException("Không tìm thấy gói xe hợp lệ");
        }
        currentCarPackage.setRemainingCars(currentCarPackage.getRemainingCars() - 1);
        userPackageRepository.save(currentCarPackage);
        
        // Tạo notification cho seller
        notificationService.createNotification(seller.getUserid(), "Đăng xe thành công",
                "Xe " + productname + " đã được đăng. Đang chờ kiểm định.");
        
        // TODO: Tạo notification cho manager để duyệt
        
        return product;
    }

    //Đăng pin (không cần kiểm định, hiển thị ngay)
    @Override
    @Transactional
    public Product createBatteryProduct(Long sellerId, String productname, String description, double cost,
                                       double capacity, double voltage, String brand, String condition,
                                       String pickupAddress, MultipartFile[] images) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        // Tạo product
        Product product = new Product();
        product.setProductname(productname);
        product.setDescription(description);
        product.setCost(cost);
        product.setAmount(1);
        product.setStatus(ProductStatus.DANG_BAN); // Pin hiển thị ngay lập tức
        product.setModel(brand);
        product.setType("Battery");
        product.setSpecs("Capacity: " + capacity + "kWh, Voltage: " + voltage + "V, Condition: " + condition);
        product.setCreatedat(new Date());
        product.setInWarehouse(false);
        product.setUsers(seller);
        
        product = productRepository.save(product);
        
        // Tạo Brandbattery
        Brandbattery brandbattery = new Brandbattery();
        brandbattery.setBrand(brand);
        brandbattery.setCapacity(capacity);
        brandbattery.setVoltage(voltage);
        brandbattery.setCondition(condition);
        brandbattery.setPickupAddress(pickupAddress);
        brandbattery.setProducts(product);
        brandBatteryRepository.save(brandbattery);
        
        // Upload images
        if (images != null && images.length > 0) {
            uploadImages(product, images);
        }
        
        //Giảm lượt đăng từ gói PIN
        UserPackage currentBatteryPackage = getCurrentPackageByType(sellerId, "BATTERY");
        if (currentBatteryPackage == null) {
            throw new RuntimeException("Không tìm thấy gói pin hợp lệ");
        }
        currentBatteryPackage.setRemainingBatteries(currentBatteryPackage.getRemainingBatteries() - 1);
        userPackageRepository.save(currentBatteryPackage);
        
        // Tạo notification cho seller
        notificationService.createNotification(seller.getUserid(), "Đăng pin thành công",
                "Pin " + productname + " đã được đăng và hiển thị ngay lập tức.");
        
        return product;
    }

    //Lấy danh sách sản phẩm xe của seller
    @Override
    public List<Product> getSellerCarProducts(Long sellerId) {
        List<Product> allProducts = productRepository.findByUsers_Userid(sellerId);
        return allProducts.stream()
                .filter(p -> "Car EV".equals(p.getType()))
                .collect(Collectors.toList());
    }

    //Cập nhật sản phẩm pin (chỉ khi chưa có đơn hàng)
    @Override
    @Transactional
    public Product updateBatteryProduct(Long sellerId, Long productId, Product updatedProduct) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Kiểm tra quyền sở hữu
        if (!product.getUsers().getUserid().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa sản phẩm này");
        }
        
        // Kiểm tra loại sản phẩm
        if (!"Battery".equals(product.getType())) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa sản phẩm pin");
        }
        
        // Kiểm tra đã có đơn hàng chưa
        if (!product.getOrder_detail().isEmpty()) {
            throw new RuntimeException("Không thể chỉnh sửa sản phẩm đã có đơn hàng");
        }
        
        // Cập nhật thông tin
        product.setProductname(updatedProduct.getProductname());
        product.setDescription(updatedProduct.getDescription());
        product.setCost(updatedProduct.getCost());
        product.setSpecs(updatedProduct.getSpecs());
        product.setUpdatedat(new Date());
        
        return productRepository.save(product);
    }

    //Xóa sản phẩm pin (chỉ khi chưa có đơn hàng)
    @Override
    @Transactional
    public void deleteBatteryProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Kiểm tra quyền sở hữu
        if (!product.getUsers().getUserid().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền xóa sản phẩm này");
        }
        
        // Kiểm tra loại sản phẩm
        if (!"Battery".equals(product.getType())) {
            throw new RuntimeException("Chỉ có thể xóa sản phẩm pin");
        }
        
        // Kiểm tra đã có đơn hàng chưa
        if (!product.getOrder_detail().isEmpty()) {
            throw new RuntimeException("Không thể xóa sản phẩm đã có đơn hàng");
        }
        
        productRepository.delete(product);
    }

    //Lấy đơn hàng xe của seller
    @Override
    public List<Orders> getSellerCarOrders(Long sellerId) {
        List<Orders> allOrders = orderRepository.findAll();
        
        return allOrders.stream()
                .filter(order -> order.getDetails().stream()
                        .anyMatch(detail -> {
                            Product product = detail.getProducts();
                            return "Car EV".equals(product.getType()) && 
                                   product.getUsers().getUserid().equals(sellerId);
                        }))
                .collect(Collectors.toList());
    }

    //Lấy đơn hàng pin của seller
    @Override
    public List<Orders> getSellerBatteryOrders(Long sellerId) {
        List<Orders> allOrders = orderRepository.findAll();
        
        return allOrders.stream()
                .filter(order -> order.getDetails().stream()
                        .anyMatch(detail -> {
                            Product product = detail.getProducts();
                            return "Battery".equals(product.getType()) && 
                                   product.getUsers().getUserid().equals(sellerId);
                        }))
                .collect(Collectors.toList());
    }

    //Thống kê doanh thu và hoạt động của seller
    @Override
    public Map<String, Object> getSellerStatistics(Long sellerId) {
        List<Product> products = productRepository.findByUsers_Userid(sellerId);
        
        long totalCars = products.stream().filter(p -> "Car EV".equals(p.getType())).count();
        long totalBatteries = products.stream().filter(p -> "Battery".equals(p.getType())).count();
        
        // Tinh tong luot xem tat ca san pham
        int totalViews = products.stream()
                .mapToInt(Product::getViewCount)
                .sum();

        // Luot xem cho xe
        int carViews = products.stream()
                .filter(p -> "Car EV".equals(p.getType()))
                .mapToInt(Product::getViewCount)
                .sum();

        // Luot xem cho pin
        int batteryViews = products.stream()
                .filter(p -> "Battery".equals(p.getType()))
                .mapToInt(Product::getViewCount)
                .sum();

        // San pham co luot xem cao nhat
        Product mostViewedProduct = products.stream()
                .max(Comparator.comparingInt(Product::getViewCount))
                .orElse(null);

        // Dem don hang hoan tat
        List<Orders> carOrders = getSellerCarOrders(sellerId);
        List<Orders> batteryOrders = getSellerBatteryOrders(sellerId);
        
        long completedCarOrders = carOrders.stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .count();
        
        long completedBatteryOrders = batteryOrders.stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .count();
        
        // Tinh doanh thu
        Map<String, Object> revenue = getRevenueDetails(sellerId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", products.size());
        stats.put("totalCars", totalCars);
        stats.put("totalBatteries", totalBatteries);
        stats.put("completedCarOrders", completedCarOrders);
        stats.put("completedBatteryOrders", completedBatteryOrders);
        stats.put("totalRevenue", revenue.get("totalRevenue"));
        stats.put("netRevenue", revenue.get("netRevenue"));
        
        // Views tracking
        stats.put("totalViews", totalViews);
        stats.put("carViews", carViews);
        stats.put("batteryViews", batteryViews);
        stats.put("averageViewsPerProduct", products.isEmpty() ? 0 : totalViews / products.size());

        if (mostViewedProduct != null) {
            stats.put("mostViewedProduct", Map.of(
                "productId", mostViewedProduct.getProductid(),
                "productName", mostViewedProduct.getProductname(),
                "views", mostViewedProduct.getViewCount(),
                "type", mostViewedProduct.getType()
            ));
        } else {
            stats.put("mostViewedProduct", null);
        }

        return stats;
    }

    //Chi tiết doanh thu với hoa hồng
    @Override
    public Map<String, Object> getRevenueDetails(Long sellerId) {
        List<Orders> carOrders = getSellerCarOrders(sellerId);
        List<Orders> batteryOrders = getSellerBatteryOrders(sellerId);
        
        // Tính tổng doanh thu từ xe
        double carRevenue = carOrders.stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .flatMap(o -> o.getDetails().stream())
                .filter(d -> "Car EV".equals(d.getProducts().getType()))
                .mapToDouble(d -> d.getUnit_price() * d.getQuantity())
                .sum();
        
        // Tính tổng doanh thu từ pin
        double batteryRevenue = batteryOrders.stream()
                .filter(o -> OrderStatus.DA_HOAN_TAT.equals(o.getStatus()))
                .flatMap(o -> o.getDetails().stream())
                .filter(d -> "Battery".equals(d.getProducts().getType()))
                .mapToDouble(d -> d.getUnit_price() * d.getQuantity())
                .sum();
        
        double totalRevenue = carRevenue + batteryRevenue;
        double commission = totalRevenue * 0.05; // 5% hoa hồng
        double netRevenue = totalRevenue - commission;
        
        Map<String, Object> revenue = new HashMap<>();
        revenue.put("carRevenue", carRevenue);
        revenue.put("batteryRevenue", batteryRevenue);
        revenue.put("totalRevenue", totalRevenue);
        revenue.put("commission", commission);
        revenue.put("commissionRate", "5%");
        revenue.put("netRevenue", netRevenue);
        
        return revenue;
    }

    // =============== HELPER METHODS ==================================================================================
    
    private void uploadImages(Product product, MultipartFile[] images) {
        for (MultipartFile image : images) {
            try {
                String url = imageUploadService.uploadImage(image, "ev_products/" + product.getProductid());
                product_img img = new product_img();
                img.setUrl(url);
                img.setProducts(product);
                productImgRepository.save(img);
            } catch (IOException e) {
                System.err.println("Upload failed for " + image.getOriginalFilename() + ": " + e.getMessage());
            }
        }
    }

    //VALIDATE PACKAGE TYPE
    //Đảm bảo gói xe không có maxBatteries, gói pin không có maxCars
    private void validatePackageType(PackageService packageService) {
        if ("CAR".equals(packageService.getPackageType())) {
            if (packageService.getMaxBatteries() > 0) {
                throw new RuntimeException("Gói xe không được có maxBatteries");
            }
            if (packageService.getMaxCars() == 0) {
                throw new RuntimeException("Gói xe phải có maxCars > 0");
            }
        } else if ("BATTERY".equals(packageService.getPackageType())) {
            if (packageService.getMaxCars() > 0) {
                throw new RuntimeException("Gói pin không được có maxCars");
            }
            if (packageService.getMaxBatteries() == 0) {
                throw new RuntimeException("Gói pin phải có maxBatteries > 0");
            }
        } else {
            throw new RuntimeException("packageType phải là CAR hoặc BATTERY");
        }
    }
}
