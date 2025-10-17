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
import com.project.tradingev_batter.dto.SellerInfoResponse;
import com.project.tradingev_batter.enums.ProductStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.project.tradingev_batter.Entity.product_img;
import org.springframework.web.bind.annotation.RequestPart;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/products")
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

    // Create product with optional images
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createProduct(
            @RequestParam("productname") String productname,
            @RequestParam("description") String description,
            @RequestParam("cost") double cost,
            @RequestParam("amount") int amount,
            @RequestParam("status") String statusStr,
            @RequestParam("model") String model,
            @RequestParam("type") String type,
            @RequestParam("specs") String specs,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        // Convert DTO to Entity
        Product product = new Product();
        product.setProductname(productname);
        product.setDescription(description);
        product.setCost(cost);
        product.setAmount(amount);

        // Parse String to ProductStatus enum
        try {
            ProductStatus status = ProductStatus.valueOf(statusStr);
            product.setStatus(status);
        } catch (IllegalArgumentException e) {
            // Default to CHO_DUYET if invalid status
            product.setStatus(ProductStatus.CHO_DUYET);
        }

        product.setModel(model);
        product.setType(type);
        product.setSpecs(specs);
        product.setCreatedat(new Date());

        Product savedProduct = productService.createProduct(product);

        // Upload images if provided
        if (images != null && images.length > 0) {
            System.out.println("Uploading " + images.length + " images for product " + savedProduct.getProductid());  // Log check if trigger
            uploadImagesForProduct(savedProduct, images);
        } else {
            System.out.println("No images provided");  // Log if no files
        }

        savedProduct = productService.getProductById(savedProduct.getProductid());

        return ResponseEntity.ok(savedProduct);
    }

    // Update product with optional new images
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestParam("productname") String productname,
            @RequestParam("description") String description,
            @RequestParam("cost") double cost,
            @RequestParam("amount") int amount,
            @RequestParam("status") String statusStr,
            @RequestParam("model") String model,
            @RequestParam("type") String type,
            @RequestParam("specs") String specs,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        // Convert DTO to Entity
        Product product = new Product();
        product.setProductname(productname);
        product.setDescription(description);
        product.setCost(cost);
        product.setAmount(amount);

        // Parse String to ProductStatus enum
        try {
            ProductStatus status = ProductStatus.valueOf(statusStr);
            product.setStatus(status);
        } catch (IllegalArgumentException e) {
            // Keep existing status if invalid
            Product existing = productService.getProductById(id);
            product.setStatus(existing.getStatus());
        }

        product.setModel(model);
        product.setType(type);
        product.setSpecs(specs);

        Product updatedProduct = productService.updateProduct(id, product);

        // Upload new images if provided
        if (images != null && images.length > 0) {
            System.out.println("Uploading " + images.length + " images for update product " + id);
            uploadImagesForProduct(updatedProduct, images);
        }

        updatedProduct = productService.getProductById(id);

        return ResponseEntity.ok(updatedProduct);
    }

    // Separate endpoint for uploading images to existing product
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProductImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] images) {

        Product product = productService.getProductById(id);
        System.out.println("Uploading separate " + images.length + " images for product " + id);  // Log
        int uploadedCount = uploadImagesForProduct(product, images);

        return ResponseEntity.ok("Successfully uploaded " + uploadedCount + " images");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // Private helper method to handle image uploads
    private int uploadImagesForProduct(Product product, MultipartFile[] images) {
        int uploadedCount = 0;
        for (MultipartFile image : images) {
            try {
                String url = imageUploadService.uploadImage(image, "ev_products/" + product.getProductid());
                product_img img = new product_img();
                img.setUrl(url);
                img.setProducts(product);
                productImgRepository.save(img);
                uploadedCount++;
                System.out.println("Saved img URL: " + url);  // Log per image
            } catch (IOException e) {
                System.err.println("Upload fail for " + image.getOriginalFilename() + ": " + e.getMessage());  // Log error
            }
        }
        return uploadedCount;
    }
}

