package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.ProductImgRepository;
import com.project.tradingev_batter.Service.FeedbackService;
import com.project.tradingev_batter.Service.ImageUploadService;
import com.project.tradingev_batter.Service.ProductService;
import com.project.tradingev_batter.Service.UserService;
import com.project.tradingev_batter.dto.ProductDetailResponse;
import com.project.tradingev_batter.dto.ProductRequest;
import com.project.tradingev_batter.dto.SellerInfoResponse;
import com.project.tradingev_batter.enums.ProductStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.project.tradingev_batter.Entity.product_img;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product APIs (Public/Manager)",
     description = " API này CHỈ dành cho:\n" +
                   "- Guest/Public: XEM sản phẩm (GET)\n" +
                   "- Manager: XÓA sản phẩm vi phạm (DELETE)\n\n" +
                   " Seller KHÔNG dùng API này để đăng bài!\n" +
                   " Seller đăng bài tại:\n" +
                   "- POST /api/seller/products/cars (đăng xe)\n" +
                   "- POST /api/seller/products/batteries (đăng pin)")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final FeedbackService feedbackService;

    @Autowired
    private ImageUploadService imageUploadService;

    @Autowired
    private ProductImgRepository productImgRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductController(ProductService productService, FeedbackService feedbackService, UserService userService) {
        this.productService = productService;
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @Operation(
            summary = "Lấy danh sách tất cả sản phẩm",
            description = "Public API - Guest có thể xem tất cả sản phẩm đang bán (DANG_BAN)"
    )
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(
            summary = "Tìm kiếm và lọc sản phẩm",
            description = "Public API - Tìm kiếm theo loại, hãng, năm, dung lượng, giá"
    )
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchAndFilter(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false) Double capacityMin,
            @RequestParam(required = false) Double capacityMax,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax) {
        return ResponseEntity.ok(productService.searchAndFilterProducts(type, brand, yearMin, yearMax, capacityMin, capacityMax, status, priceMin, priceMax));
    }

    @Operation(
            summary = "Lấy chi tiết sản phẩm",
            description = "Public API - Lấy thông tin chi tiết của một sản phẩm (tự động tăng view count)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductById(
            @Parameter(description = "ID của sản phẩm", required = true)
            @PathVariable Long id) {
        Product product = productService.getProductById(id);

        // Tự động tăng viewCount khi user/guest xem chi tiết sản phẩm
        try {
            productService.incrementViewCount(id);
        } catch (Exception e) {
            // Log error nhưng vẫn trả về product detail
            System.err.println("Failed to increment view count: " + e.getMessage());
        }

        List<Feedback> feedbacks = feedbackService.getFeedbacksByProduct(id);
        ProductDetailResponse response = new ProductDetailResponse(product, feedbacks);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Lấy thông tin Seller và sản phẩm của họ",
            description = "Public API - Xem profile người bán và danh sách sản phẩm đang bán"
    )
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<SellerInfoResponse> getSellerInfo(@PathVariable Long sellerId) {
        User seller = userService.getUserById(sellerId);
        List<Product> products = productService.getProductsBySeller(sellerId);
        SellerInfoResponse response = new SellerInfoResponse(seller.getUsername(), seller.getDisplayname(), products);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[MANAGER ONLY] Xóa sản phẩm vi phạm",
            description = " Manager chỉ được XÓA sản phẩm vi phạm quy định.\n\n" +
                    " Manager KHÔNG được:\n" +
                    "- Tạo sản phẩm (chỉ Seller mới được đăng bài)\n" +
                    "- Sửa sản phẩm (chỉ Seller chỉnh sửa bài của mình)\n\n" +
                    " Manager được:\n" +
                    "- Xóa sản phẩm vi phạm\n" +
                    "- Duyệt/từ chối bài đăng tại /api/staff hoặc /api/manager"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @Parameter(description = "ID của sản phẩm cần xóa", required = true)
            @PathVariable Long id) {
        productService.deleteProduct(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sản phẩm đã được xóa");
        return ResponseEntity.ok(response);
    }
}

