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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final FeedbackService feedbackService;

    public ProductController(ProductService productService, FeedbackService feedbackService, UserService userService) {
        this.productService = productService;
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        List<Feedback> feedbacks = feedbackService.getFeedbacksByProduct(id);
        ProductDetailResponse response = new ProductDetailResponse(product, feedbacks);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<SellerInfoResponse> getSellerInfo(@PathVariable Long sellerId) {
        User seller = userService.getUserById(sellerId);
        List<Product> products = productService.getProductsBySeller(sellerId);
        SellerInfoResponse response = new SellerInfoResponse(seller.getUsername(), seller.getDisplayname(), products);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
}
