package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.PackagePurchaseRequest;
import com.project.tradingev_batter.dto.PriceSuggestionRequest;
import com.project.tradingev_batter.dto.PriceSuggestionResponse;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Seller APIs", description = "API dành cho người bán - Quản lý gói dịch vụ, đăng bán sản phẩm, theo dõi doanh thu, hợp đồng, feedback")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final FeedbackService feedbackService;
    private final ContractService contractService;
    private final GeminiAIService geminiAIService;

    public SellerController(SellerService sellerService, 
                           ProductService productService,
                           FeedbackService feedbackService,
                           ContractService contractService,
                           GeminiAIService geminiAIService) {
        this.sellerService = sellerService;
        this.productService = productService;
        this.feedbackService = feedbackService;
        this.contractService = contractService;
        this.geminiAIService = geminiAIService;
    }

    //Mua các gói dịch vụ đăng bán (Cơ bản, Chuyên nghiệp, VIP)
    //NOTE: API này CHỈ TẠO ĐƠN HÀNG MUA GÓI, chưa thanh toán
    //Seller cần gọi /api/payment/create-payment-url với transactionType=PACKAGE_PURCHASE để thanh toán
    @Operation(
            summary = "Mua gói dịch vụ đăng bán",
            description = "Tạo đơn hàng mua gói dịch vụ (Cơ bản, Chuyên nghiệp, VIP). Seller cần thanh toán qua VNPay để kích hoạt gói."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đơn hàng đã được tạo - Trả về hướng dẫn thanh toán"),
            @ApiResponse(responseCode = "400", description = "Gói không tồn tại hoặc không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc không có quyền Seller")
    })
    @PostMapping("/packages/purchase")
    public ResponseEntity<Map<String, Object>> purchasePackage(
            @Valid @RequestBody PackagePurchaseRequest request) {

        User seller = getCurrentUser();
        
        try {
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

        } catch (RuntimeException e) {
            // Handle business logic errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            // Check if it's a duplicate package error
            if (e.getMessage().contains("đang hoạt động")) {
                errorResponse.put("suggestion", "Bạn có thể sử dụng API /api/seller/packages/current để xem chi tiết gói hiện tại");
            }

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Theo dõi hạn sử dụng và số lượt đăng còn lại trong gói
    @Operation(
            summary = "Xem gói dịch vụ hiện tại",
            description = "Lấy thông tin gói CAR và gói BATTERY đang sử dụng (nếu có)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin gói"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @GetMapping("/packages/current")
    public ResponseEntity<Map<String, Object>> getCurrentPackage() {
        User seller = getCurrentUser();

        // Lấy gói XE
        UserPackage carPackage = sellerService.getCurrentPackageByType(seller.getUserid(), "CAR");
        // Lấy gói PIN
        UserPackage batteryPackage = sellerService.getCurrentPackageByType(seller.getUserid(), "BATTERY");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");

        // Thông tin gói XE
        if (carPackage != null) {
            response.put("carPackage", Map.of(
                    "packageName", carPackage.getPackageService().getName(),
                    "packageType", "CAR",
                    "purchaseDate", carPackage.getPurchaseDate(),
                    "expiryDate", carPackage.getExpiryDate(),
                    "remainingCars", carPackage.getRemainingCars(),
                    "isExpired", sellerService.isPackageExpired(carPackage)
            ));
        } else {
            response.put("carPackage", null);
            response.put("carPackageMessage", "Bạn chưa có gói đăng xe. Vui lòng mua gói để đăng bán xe.");
        }

        // Thông tin gói PIN
        if (batteryPackage != null) {
            response.put("batteryPackage", Map.of(
                    "packageName", batteryPackage.getPackageService().getName(),
                    "packageType", "BATTERY",
                    "purchaseDate", batteryPackage.getPurchaseDate(),
                    "expiryDate", batteryPackage.getExpiryDate(),
                    "remainingBatteries", batteryPackage.getRemainingBatteries(),
                    "isExpired", sellerService.isPackageExpired(batteryPackage)
            ));
        } else {
            response.put("batteryPackage", null);
            response.put("batteryPackageMessage", "Bạn chưa có gói đăng pin. Vui lòng mua gói để đăng bán pin.");
        }

        return ResponseEntity.ok(response);
    }

    // Endpoint riêng để lấy thông tin gói XE
    @Operation(
            summary = "Xem gói CAR hiện tại",
            description = "Lấy thông tin chi tiết gói đăng xe đang sử dụng"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Chưa có gói CAR")
    })
    @GetMapping("/packages/car")
    public ResponseEntity<Map<String, Object>> getCarPackage() {
        User seller = getCurrentUser();
        UserPackage carPackage = sellerService.getCurrentPackageByType(seller.getUserid(), "CAR");

        Map<String, Object> response = new HashMap<>();
        if (carPackage != null) {
            response.put("status", "success");
            response.put("package", Map.of(
                    "packageName", carPackage.getPackageService().getName(),
                    "packageType", "CAR",
                    "purchaseDate", carPackage.getPurchaseDate(),
                    "expiryDate", carPackage.getExpiryDate(),
                    "remainingCars", carPackage.getRemainingCars(),
                    "maxCars", carPackage.getPackageService().getMaxCars(),
                    "isExpired", sellerService.isPackageExpired(carPackage)
            ));
        } else {
            response.put("status", "error");
            response.put("message", "Bạn chưa có gói đăng xe. Vui lòng mua gói để đăng bán xe.");
        }

        return ResponseEntity.ok(response);
    }

    // Endpoint riêng để lấy thông tin gói PIN
    @Operation(
            summary = "Xem gói BATTERY hiện tại",
            description = "Lấy thông tin chi tiết gói đăng pin đang sử dụng"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Chưa có gói BATTERY")
    })
    @GetMapping("/packages/battery")
    public ResponseEntity<Map<String, Object>> getBatteryPackage() {
        User seller = getCurrentUser();
        UserPackage batteryPackage = sellerService.getCurrentPackageByType(seller.getUserid(), "BATTERY");

        Map<String, Object> response = new HashMap<>();
        if (batteryPackage != null) {
            response.put("status", "success");
            response.put("package", Map.of(
                    "packageName", batteryPackage.getPackageService().getName(),
                    "packageType", "BATTERY",
                    "purchaseDate", batteryPackage.getPurchaseDate(),
                    "expiryDate", batteryPackage.getExpiryDate(),
                    "remainingBatteries", batteryPackage.getRemainingBatteries(),
                    "maxBatteries", batteryPackage.getPackageService().getMaxBatteries(),
                    "isExpired", sellerService.isPackageExpired(batteryPackage)
            ));
        } else {
            response.put("status", "error");
            response.put("message", "Bạn chưa có gói đăng pin. Vui lòng mua gói để đăng bán pin.");
        }
        
        return ResponseEntity.ok(response);
    }

    //Đăng xe - Cung cấp đầy đủ thông tin xe, hình ảnh, biển số, thông số, tình trạng, giá
    @Operation(
            summary = "Đăng bán xe",
            description = "Seller đăng bán xe (cần có gói CAR còn hiệu lực). Xe sẽ đi qua quy trình kiểm định."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng xe thành công - Chờ kiểm định"),
            @ApiResponse(responseCode = "400", description = "Hết lượt đăng hoặc gói đã hết hạn"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem trạng thái đăng xe",
            description = "Lấy danh sách xe đã đăng và trạng thái hiện tại (CHO_DUYET, CHO_KIEM_DUYET, DANG_BAN, ...)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Gia hạn gói dịch vụ",
            description = "Gia hạn gói CAR hoặc BATTERY khi sắp hết hạn"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gia hạn thành công"),
            @ApiResponse(responseCode = "400", description = "Gói không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Đăng bán pin",
            description = "Seller đăng bán pin (cần có gói BATTERY còn hiệu lực). Pin được hiển thị ngay sau khi đăng."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng pin thành công"),
            @ApiResponse(responseCode = "400", description = "Hết lượt đăng hoặc gói đã hết hạn"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Cập nhật thông tin pin",
            description = "Chỉnh sửa thông tin pin (chỉ khi chưa có đơn hàng)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Sản phẩm đã có đơn hàng hoặc không thuộc về seller"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
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

    @Operation(
            summary = "Xóa sản phẩm pin",
            description = "Xóa sản phẩm pin (chỉ khi chưa có đơn hàng)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Sản phẩm đã có đơn hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
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
    @Operation(
            summary = "Xem đơn hàng xe",
            description = "Lấy danh sách đơn hàng xe mà seller đã bán"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem đơn hàng pin",
            description = "Lấy danh sách đơn hàng pin mà seller đã bán"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem thống kê seller",
            description = "Lấy thống kê tổng quan: số sản phẩm, lượt xem, đơn hàng, doanh thu"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem chi tiết doanh thu",
            description = "Lấy chi tiết doanh thu: tổng bán, hoa hồng, thực nhận"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem đánh giá từ người mua",
            description = "Lấy danh sách feedback và rating trung bình của seller"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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
    @Operation(
            summary = "Xem tất cả sản phẩm",
            description = "Lấy danh sách tất cả sản phẩm (xe + pin) của seller"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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

    // =============== CONTRACT MANAGEMENT (SPRINT 2.2) ================================================================

    // SELLER XEM DANH SÁCH HỢP ĐỒNG CHỜ KÝ
    // Sau khi xe đạt kiểm định, manager tạo contract -> seller cần ký
    @Operation(
            summary = "Xem hợp đồng chờ ký",
            description = "Lấy danh sách hợp đồng đang chờ seller ký (sau khi xe đạt kiểm định)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @GetMapping("/contracts/pending")
    public ResponseEntity<Map<String, Object>> getPendingContracts() {
        User seller = getCurrentUser();
        List<Contracts> pendingContracts = contractService.getPendingContracts(seller.getUserid());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("pendingContracts", pendingContracts);
        response.put("totalPending", pendingContracts.size());

        return ResponseEntity.ok(response);
    }

    // SELLER KÝ HỢP ĐỒNG
    // Seller click "Ký hợp đồng" -> redirect sang DocuSeal để ký
    @Operation(
            summary = "Ký hợp đồng",
            description = "Seller ký hợp đồng qua DocuSeal sau khi xe đạt kiểm định"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chuyển sang DocuSeal để ký"),
            @ApiResponse(responseCode = "400", description = "Hợp đồng không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @PostMapping("/contracts/{contractId}/sign")
    public ResponseEntity<Map<String, Object>> signContract(@PathVariable Long contractId) {
        User seller = getCurrentUser();

        try {
            Contracts contract = contractService.signContract(contractId, seller.getUserid());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vui lòng ký hợp đồng trên DocuSeal");
            response.put("contract", contract);
            response.put("docusealSubmissionId", contract.getDocusealSubmissionId());
            response.put("note", "Frontend cần gọi DocuSeal API để lấy signing URL");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // SELLER XEM TẤT CẢ HỢP ĐỒNG
    @Operation(
            summary = "Xem tất cả hợp đồng",
            description = "Lấy danh sách tất cả hợp đồng của seller"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @GetMapping("/contracts")
    public ResponseEntity<Map<String, Object>> getAllContracts() {
        User seller = getCurrentUser();
        List<Contracts> contracts = contractService.getSellerContracts(seller.getUserid());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("contracts", contracts);
        response.put("totalContracts", contracts.size());

        return ResponseEntity.ok(response);
    }

    // SELLER GỌI API ĐỂ GỢI Ý GIÁ DỰA TRÊN DỮ LIỆU THỊ TRƯỜNG
    // Input: productType, brand, year, condition, model (optional), capacity/mileage (optional)
    // Output: minPrice, maxPrice, suggestedPrice, marketInsight
    @Operation(
            summary = "Gợi ý giá bán dựa trên AI",
            description = "Seller nhập thông tin sản phẩm (loại, hãng, năm, tình trạng...) để nhận gợi ý giá từ AI"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về gợi ý giá"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @PostMapping("/suggest-price")
    public ResponseEntity<PriceSuggestionResponse> suggestPrice(
            @Valid @RequestBody PriceSuggestionRequest request) {

        User seller = getCurrentUser(); // Chỉ seller mới được dùng feature này

        try {
            PriceSuggestionResponse suggestion = geminiAIService.suggestPrice(request, seller.getUserid());
            return ResponseEntity.ok(suggestion);

        } catch (Exception e) {
            System.err.println("Error in price suggestion endpoint: " + e.getMessage());
            // Fallback nếu có lỗi
            PriceSuggestionResponse fallback = geminiAIService.suggestPrice(request, seller.getUserid());
            return ResponseEntity.ok(fallback);
        }
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
