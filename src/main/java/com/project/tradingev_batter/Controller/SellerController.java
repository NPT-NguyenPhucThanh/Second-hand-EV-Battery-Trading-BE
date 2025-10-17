package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.PackagePurchaseRequest;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.security.CustomUserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final FeedbackService feedbackService;

    public SellerController(SellerService sellerService, 
                           ProductService productService,
                           FeedbackService feedbackService) {
        this.sellerService = sellerService;
        this.productService = productService;
        this.feedbackService = feedbackService;
    }

    //Mua các gói dịch vụ đăng bán (Cơ bản, Chuyên nghiệp, VIP)
    //NOTE: API này CHỈ TẠO ĐƠN HÀNG MUA GÓI, chưa thanh toán
    //Seller cần gọi /api/payment/create-payment-url với transactionType=PACKAGE_PURCHASE để thanh toán
    @PostMapping("/packages/purchase")
    public ResponseEntity<Map<String, Object>> purchasePackage(
            @RequestBody PackagePurchaseRequest request) {
        
        User seller = getCurrentUser();
        
        // Tạo order mua gói (chưa active)
        Map<String, Object> purchaseResult = sellerService.createPackagePurchaseOrder(
                seller.getUserid(), 
                request.getPackageId()
        );
        
        Long orderId = (Long) purchaseResult.get("orderId");
        Double packagePrice = (Double) purchaseResult.get("packagePrice");
        String packageName = (String) purchaseResult.get("packageName");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đơn hàng mua gói đã được tạo. Vui lòng thanh toán.");
        response.put("orderId", orderId);
        response.put("packageName", packageName);
        response.put("packagePrice", packagePrice);
        response.put("nextStep", Map.of(
                "endpoint", "/api/payment/create-payment-url",
                "method", "POST",
                "params", Map.of(
                        "orderId", orderId,
                        "transactionType", "PACKAGE_PURCHASE"
                ),
                "note", "Frontend cần gọi API này để lấy paymentUrl, sau đó redirect seller sang VNPay"
        ));
        
        return ResponseEntity.ok(response);
    }

    //Theo dõi hạn sử dụng và số lượt đăng còn lại trong gói
    @GetMapping("/packages/current")
    public ResponseEntity<Map<String, Object>> getCurrentPackage() {
        User seller = getCurrentUser();
        UserPackage currentPackage = sellerService.getCurrentPackage(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        if (currentPackage != null) {
            response.put("status", "success");
            response.put("package", Map.of(
                    "packageName", currentPackage.getPackageService().getName(),
                    "purchaseDate", currentPackage.getPurchaseDate(),
                    "expiryDate", currentPackage.getExpiryDate(),
                    "remainingCars", currentPackage.getRemainingCars(),
                    "remainingBatteries", currentPackage.getRemainingBatteries(),
                    "isExpired", sellerService.isPackageExpired(currentPackage)
            ));
        } else {
            response.put("status", "error");
            response.put("message", "Bạn chưa có gói nào. Vui lòng mua gói để đăng bán sản phẩm.");
        }
        
        return ResponseEntity.ok(response);
    }

    //Đăng xe - Cung cấp đầy đủ thông tin xe, hình ảnh, biển số, thông số, tình trạng, giá
    @PostMapping(value = "/products/cars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createCarProduct(
            @RequestParam("productname") String productname,
            @RequestParam("description") String description,
            @RequestParam("cost") double cost,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("model") String model,
            @RequestParam("specs") String specs,
            @RequestParam("brand") String brand,
            @RequestParam("year") int year,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        User seller = getCurrentUser();
        
        // Kiểm tra gói còn hiệu lực
        if (!sellerService.canPostCar(seller.getUserid())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Bạn đã hết lượt đăng xe hoặc gói đã hết hạn. Vui lòng mua/gia hạn gói.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Product product = sellerService.createCarProduct(
                seller.getUserid(), productname, description, cost, 
                licensePlate, model, specs, brand, year, images);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đăng xe thành công. Đang chờ kiểm định.");
        response.put("product", product);
        
        return ResponseEntity.ok(response);
    }

    //Quản lý trạng thái đăng xe
    @GetMapping("/products/cars/status")
    public ResponseEntity<Map<String, Object>> getCarProductsStatus() {
        User seller = getCurrentUser();
        List<Product> products = sellerService.getSellerCarProducts(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("products", products.stream().map(p -> Map.of(
                "productId", p.getProductid(),
                "productName", p.getProductname(),
                "status", p.getStatus(),
                "statusDescription", getStatusDescription(p.getStatus()),
                "createdAt", p.getCreatedat(),
                "inWarehouse", p.getInWarehouse()
        )).toList());
        
        return ResponseEntity.ok(response);
    }

    //Gia hạn gói để tiếp tục hiển thị xe nếu sắp hết hạn
    @PostMapping("/packages/renew")
    public ResponseEntity<Map<String, Object>> renewPackage(
            @RequestBody PackagePurchaseRequest request) {
        
        User seller = getCurrentUser();
        UserPackage renewedPackage = sellerService.renewPackage(seller.getUserid(), request.getPackageId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Gia hạn gói thành công");
        response.put("package", renewedPackage);
        
        return ResponseEntity.ok(response);
    }

    //Đăng bán pin - Cung cấp thông tin cơ bản, không cần kiểm định hoặc hợp đồng
    @PostMapping(value = "/products/batteries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createBatteryProduct(
            @RequestParam("productname") String productname,
            @RequestParam("description") String description,
            @RequestParam("cost") double cost,
            @RequestParam("capacity") double capacity,
            @RequestParam("voltage") double voltage,
            @RequestParam("brand") String brand,
            @RequestParam("condition") String condition,
            @RequestParam("pickupAddress") String pickupAddress,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        User seller = getCurrentUser();
        
        // Kiểm tra gói còn hiệu lực
        if (!sellerService.canPostBattery(seller.getUserid())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Bạn đã hết lượt đăng pin hoặc gói đã hết hạn. Vui lòng mua/gia hạn gói.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Product product = sellerService.createBatteryProduct(
                seller.getUserid(), productname, description, cost, 
                capacity, voltage, brand, condition, pickupAddress, images);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đăng pin thành công. Sản phẩm đã hiển thị ngay.");
        response.put("product", product);
        
        return ResponseEntity.ok(response);
    }

    //Quản lý sản phẩm pin - Chỉnh sửa hoặc gỡ khi chưa có đơn hàng
    @PutMapping("/products/batteries/{id}")
    public ResponseEntity<Map<String, Object>> updateBatteryProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {
        
        User seller = getCurrentUser();
        Product product = sellerService.updateBatteryProduct(seller.getUserid(), id, updatedProduct);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cập nhật pin thành công");
        response.put("product", product);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/batteries/{id}")
    public ResponseEntity<Map<String, Object>> deleteBatteryProduct(@PathVariable Long id) {
        User seller = getCurrentUser();
        sellerService.deleteBatteryProduct(seller.getUserid(), id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Xóa pin thành công");
        
        return ResponseEntity.ok(response);
    }

    //Theo dõi đơn hàng xe đang trong quá trình giao dịch
    @GetMapping("/orders/cars")
    public ResponseEntity<Map<String, Object>> getCarOrders() {
        User seller = getCurrentUser();
        List<Orders> orders = sellerService.getSellerCarOrders(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("orders", orders);
        
        return ResponseEntity.ok(response);
    }

    //Theo dõi đơn hàng pin (giao hàng, xác nhận, hoàn tất)
    @GetMapping("/orders/batteries")
    public ResponseEntity<Map<String, Object>> getBatteryOrders() {
        User seller = getCurrentUser();
        List<Orders> orders = sellerService.getSellerBatteryOrders(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("orders", orders);
        
        return ResponseEntity.ok(response);
    }

    //Xem thống kê doanh thu từ xe, pin, lượt xem, và đánh giá từ người mua
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSellerStatistics() {
        User seller = getCurrentUser();
        Map<String, Object> statistics = sellerService.getSellerStatistics(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("statistics", statistics);
        
        return ResponseEntity.ok(response);
    }

    //Hiển thị rõ các khoản hoa hồng bị trừ và số tiền thực nhận
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueDetails() {
        User seller = getCurrentUser();
        Map<String, Object> revenue = sellerService.getRevenueDetails(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("revenue", revenue);
        
        return ResponseEntity.ok(response);
    }

    // Quản lý các đánh giá nhận được từ người mua
    @GetMapping("/feedbacks")
    public ResponseEntity<Map<String, Object>> getSellerFeedbacks() {
        User seller = getCurrentUser();
        List<Feedback> feedbacks = feedbackService.getFeedbacksForSeller(seller.getUserid());
        
        double avgRating = feedbackService.getAverageRatingBySeller(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("averageRating", avgRating);
        response.put("totalReviews", feedbacks.size());
        response.put("feedbacks", feedbacks);
        
        return ResponseEntity.ok(response);
    }

    //Lấy tất cả sản phẩm của seller
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllSellerProducts() {
        User seller = getCurrentUser();
        List<Product> products = productService.getProductsBySeller(seller.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalProducts", products.size());
        response.put("products", products);
        
        return ResponseEntity.ok(response);
    }

    // =============== HELPER METHODS ==================================================================================
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        return userDetails.getUser();
    }

    private String getStatusDescription(ProductStatus status) {
        return switch (status) {
            case CHO_DUYET -> "Chờ duyệt sơ bộ";
            case CHO_KIEM_DUYET -> "Chờ kiểm định";
            case DA_DUYET -> "Đã duyệt - Chờ ký hợp đồng";
            case DANG_BAN -> "Đang bán";
            case BI_TU_CHOI -> "Bị từ chối";
            case KHONG_DAT_KIEM_DINH -> "Không đạt kiểm định";
            case HET_HAN -> "Hết hạn";
            case REMOVED_FROM_WAREHOUSE -> "Đã gỡ khỏi kho";
            default -> "Không xác định";
        };
    }
}
