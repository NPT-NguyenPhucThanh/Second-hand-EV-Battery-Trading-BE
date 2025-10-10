package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.FeedbackService;
import com.project.tradingev_batter.Service.ProductService;
import com.project.tradingev_batter.Service.UserService;
import com.project.tradingev_batter.dto.ProductDetailResponse;
import com.project.tradingev_batter.dto.SellerInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/guest")
public class GuestController {

    private final ProductService productService;
    private final UserService userService;
    private final FeedbackService feedbackService;

    public GuestController(ProductService productService, UserService userService, FeedbackService feedbackService) {
        this.productService = productService;
        this.userService = userService;
        this.feedbackService = feedbackService;
    }


     //Guest có thể xem danh sách sản phẩm xe điện và pin đã qua sử dụng

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        List<Product> products = productService.getAllActiveProducts();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("products", products);
        response.put("total", products.size());
        response.put("page", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }

    //Tìm kiếm và lọc sản phẩm theo hãng, năm, dung lượng, tình trạng, giá, v.v.
    @GetMapping("/products/search")
    public ResponseEntity<Map<String, Object>> searchAndFilter(
            @RequestParam(required = false) String type,           // "Car EV", "Battery"
            @RequestParam(required = false) String brand,          // Hãng xe/pin
            @RequestParam(required = false) Integer yearMin,       // Năm tối thiểu
            @RequestParam(required = false) Integer yearMax,       // Năm tối đa
            @RequestParam(required = false) Double capacityMin,    // Dung lượng tối thiểu
            @RequestParam(required = false) Double capacityMax,    // Dung lượng tối đa
            @RequestParam(required = false) String status,         // Tình trạng
            @RequestParam(required = false) Double priceMin,       // Giá tối thiểu
            @RequestParam(required = false) Double priceMax) {     // Giá tối đa
        
        List<Product> products = productService.searchAndFilterProducts(
                type, brand, yearMin, yearMax, capacityMin, capacityMax, status, priceMin, priceMax);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("products", products);
        response.put("total", products.size());
        response.put("filters", Map.of(
                "type", type,
                "brand", brand,
                "yearRange", yearMin + " - " + yearMax,
                "capacityRange", capacityMin + " - " + capacityMax,
                "priceRange", priceMin + " - " + priceMax
        ));
        
        return ResponseEntity.ok(response);
    }


    //Xem chi tiết sản phẩm (mô tả, thông số, đánh giá)
    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        List<Feedback> feedbacks = feedbackService.getFeedbacksByProduct(id);
        
        ProductDetailResponse productDetail = new ProductDetailResponse(product, feedbacks);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("product", productDetail);
        
        return ResponseEntity.ok(response);
    }

    //Xem thông tin người bán và các sản phẩm họ đang đăng
    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<Map<String, Object>> getSellerInfo(@PathVariable Long sellerId) {
        User seller = userService.getUserById(sellerId);
        List<Product> products = productService.getProductsBySeller(sellerId);
        
        // Tính rating trung bình của seller
        double avgRating = feedbackService.getAverageRatingBySeller(sellerId);
        int totalReviews = feedbackService.getTotalReviewsBySeller(sellerId);
        
        SellerInfoResponse sellerInfo = new SellerInfoResponse(
                seller.getUsername(), 
                seller.getDisplayname(), 
                products
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("seller", Map.of(
                "username", seller.getUsername(),
                "displayname", seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername(),
                "email", seller.getEmail(),
                "phone", seller.getPhone() != null ? seller.getPhone() : "N/A",
                "memberSince", seller.getCreated_at(),
                "averageRating", avgRating,
                "totalReviews", totalReviews,
                "totalProducts", products.size()
        ));
        response.put("products", products);
        
        return ResponseEntity.ok(response);
    }

    //Endpoint thông báo Guest cần đăng nhập để thực hiện hành động
    @GetMapping("/require-login")
    public ResponseEntity<Map<String, Object>> requireLogin() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Bạn cần đăng nhập để thực hiện hành động này");
        response.put("loginUrl", "/api/auth/login");
        response.put("registerUrl", "/api/auth/register");
        
        return ResponseEntity.status(401).body(response);
    }

    //Lấy danh sách hãng xe/pin
    @GetMapping("/brands")
    public ResponseEntity<Map<String, Object>> getBrands(@RequestParam String type) {
        List<String> brands = productService.getBrandsByType(type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("type", type);
        response.put("brands", brands);
        
        return ResponseEntity.ok(response);
    }

    //Lấy thống kê tổng quan
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPublicStatistics() {
        Map<String, Object> stats = productService.getPublicStatistics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("statistics", stats);
        
        return ResponseEntity.ok(response);
    }
}
