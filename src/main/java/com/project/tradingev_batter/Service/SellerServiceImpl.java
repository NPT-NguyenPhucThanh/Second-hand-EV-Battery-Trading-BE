package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
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
    private final OrderDetailRepository orderDetailRepository;
    private final ImageUploadService imageUploadService;
    private final NotificationRepository notificationRepository;

    public SellerServiceImpl(UserRepository userRepository,
                            PackageServiceRepository packageServiceRepository,
                            UserPackageRepository userPackageRepository,
                            ProductRepository productRepository,
                            BrandcarsRepository brandcarsRepository,
                            BrandBatteryRepository brandBatteryRepository,
                            ProductImgRepository productImgRepository,
                            OrderRepository orderRepository,
                            OrderDetailRepository orderDetailRepository,
                            ImageUploadService imageUploadService,
                            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.packageServiceRepository = packageServiceRepository;
        this.userPackageRepository = userPackageRepository;
        this.productRepository = productRepository;
        this.brandcarsRepository = brandcarsRepository;
        this.brandBatteryRepository = brandBatteryRepository;
        this.productImgRepository = productImgRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.imageUploadService = imageUploadService;
        this.notificationRepository = notificationRepository;
    }

    //Mua gói dịch vụ đăng bán
    @Override
    @Transactional
    public UserPackage purchasePackage(Long sellerId, Long packageId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        PackageService packageService = packageServiceRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));
        
        // Tạo UserPackage mới
        UserPackage userPackage = new UserPackage();
        userPackage.setUser(seller);
        userPackage.setPackageService(packageService);
        userPackage.setPurchaseDate(new Date());
        
        // Tính expiry date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, packageService.getDurationMonths());
        userPackage.setExpiryDate(calendar.getTime());
        
        // Set số lượt đăng
        userPackage.setRemainingCars(packageService.getMaxCars());
        userPackage.setRemainingBatteries(packageService.getMaxBatteries());
        
        userPackageRepository.save(userPackage);
        
        // Tạo notification
        createNotification(seller, "Mua gói thành công", 
                "Bạn đã mua gói " + packageService.getName() + " thành công. Hạn sử dụng đến " + userPackage.getExpiryDate());
        
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
            // Gia hạn từ ngày hết hạn hiện tại
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentPackage.getExpiryDate());
            calendar.add(Calendar.MONTH, packageService.getDurationMonths());
            currentPackage.setExpiryDate(calendar.getTime());
            
            // Cộng thêm lượt đăng
            currentPackage.setRemainingCars(currentPackage.getRemainingCars() + packageService.getMaxCars());
            currentPackage.setRemainingBatteries(currentPackage.getRemainingBatteries() + packageService.getMaxBatteries());
            
            userPackageRepository.save(currentPackage);
            
            createNotification(seller, "Gia hạn gói thành công", 
                    "Gói của bạn đã được gia hạn đến " + currentPackage.getExpiryDate());
            
            return currentPackage;
        } else {
            // Mua gói mới
            return purchasePackage(sellerId, packageId);
        }
    }

    @Override
    public boolean canPostCar(Long sellerId) {
        UserPackage currentPackage = getCurrentPackage(sellerId);
        return currentPackage != null && 
               !isPackageExpired(currentPackage) && 
               currentPackage.getRemainingCars() > 0;
    }

    @Override
    public boolean canPostBattery(Long sellerId) {
        UserPackage currentPackage = getCurrentPackage(sellerId);
        return currentPackage != null && 
               !isPackageExpired(currentPackage) && 
               currentPackage.getRemainingBatteries() > 0;
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
        product.setStatus("CHO_DUYET"); // Chờ duyệt sơ bộ
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
        
        // Giảm lượt đăng
        UserPackage currentPackage = getCurrentPackage(sellerId);
        currentPackage.setRemainingCars(currentPackage.getRemainingCars() - 1);
        userPackageRepository.save(currentPackage);
        
        // Tạo notification cho seller
        createNotification(seller, "Đăng xe thành công", 
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
        product.setStatus("DANG_BAN"); // Pin hiển thị ngay lập tức
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
        
        // Giảm lượt đăng
        UserPackage currentPackage = getCurrentPackage(sellerId);
        currentPackage.setRemainingBatteries(currentPackage.getRemainingBatteries() - 1);
        userPackageRepository.save(currentPackage);
        
        // Tạo notification cho seller
        createNotification(seller, "Đăng pin thành công", 
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
        if (product.getUsers().getUserid() != sellerId) {
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
        if (product.getUsers().getUserid() != sellerId) {
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
                                   product.getUsers().getUserid() == sellerId;
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
                                   product.getUsers().getUserid() == sellerId;
                        }))
                .collect(Collectors.toList());
    }

    //Thống kê doanh thu và hoạt động của seller
    @Override
    public Map<String, Object> getSellerStatistics(Long sellerId) {
        List<Product> products = productRepository.findByUsers_Userid(sellerId);
        
        long totalCars = products.stream().filter(p -> "Car EV".equals(p.getType())).count();
        long totalBatteries = products.stream().filter(p -> "Battery".equals(p.getType())).count();
        
        // Đếm đơn hàng hoàn tất
        List<Orders> carOrders = getSellerCarOrders(sellerId);
        List<Orders> batteryOrders = getSellerBatteryOrders(sellerId);
        
        long completedCarOrders = carOrders.stream()
                .filter(o -> "DA_HOAN_TAT".equals(o.getStatus()))
                .count();
        
        long completedBatteryOrders = batteryOrders.stream()
                .filter(o -> "DA_HOAN_TAT".equals(o.getStatus()))
                .count();
        
        // Tính doanh thu
        Map<String, Object> revenue = getRevenueDetails(sellerId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", products.size());
        stats.put("totalCars", totalCars);
        stats.put("totalBatteries", totalBatteries);
        stats.put("completedCarOrders", completedCarOrders);
        stats.put("completedBatteryOrders", completedBatteryOrders);
        stats.put("totalRevenue", revenue.get("totalRevenue"));
        stats.put("netRevenue", revenue.get("netRevenue"));
        
        return stats;
    }

    //Chi tiết doanh thu với hoa hồng
    @Override
    public Map<String, Object> getRevenueDetails(Long sellerId) {
        List<Orders> carOrders = getSellerCarOrders(sellerId);
        List<Orders> batteryOrders = getSellerBatteryOrders(sellerId);
        
        // Tính tổng doanh thu từ xe
        double carRevenue = carOrders.stream()
                .filter(o -> "DA_HOAN_TAT".equals(o.getStatus()))
                .flatMap(o -> o.getDetails().stream())
                .filter(d -> "Car EV".equals(d.getProducts().getType()))
                .mapToDouble(d -> d.getUnit_price() * d.getQuantity())
                .sum();
        
        // Tính tổng doanh thu từ pin
        double batteryRevenue = batteryOrders.stream()
                .filter(o -> "DA_HOAN_TAT".equals(o.getStatus()))
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

    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }
}
