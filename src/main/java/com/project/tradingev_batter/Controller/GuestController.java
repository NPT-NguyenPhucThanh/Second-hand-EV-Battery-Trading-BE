package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Feedback;
import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.PackageServiceService;
import com.project.tradingev_batter.Service.ProductService;
import com.project.tradingev_batter.Service.UserService;
import com.project.tradingev_batter.enums.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GuestController - API công khai cho người dùng chưa đăng nhập
 * Xem danh sách sản phẩm
 * Tìm kiếm và lọc sản phẩm
 * Xem chi tiết sản phẩm
 * Xem thông tin người bán
 * Xem danh sách gói dịch vụ
 * Yêu cầu đăng nhập khi mua hoặc chat
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Guest APIs", description = "API công khai cho người dùng chưa đăng nhập - Xem sản phẩm, tìm kiếm, lọc")
public class GuestController {

    private final ProductService productService;
    private final UserService userService;
    private final PackageServiceService packageServiceService;

    public GuestController(ProductService productService, UserService userService, PackageServiceService packageServiceService) {
        this.productService = productService;
        this.userService = userService;
        this.packageServiceService = packageServiceService;
    }

    //Xem danh sách tất cả sản phẩm (xe và pin đang bán)
    //Chỉ hiển thị sản phẩm có status = DANG_BAN
    @Operation(
            summary = "Xem danh sách sản phẩm",
            description = "Lấy danh sách tất cả sản phẩm đang bán (DANG_BAN). Hỗ trợ lọc theo loại và phân trang."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách sản phẩm với pagination"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @Parameter(description = "Loại sản phẩm: 'Car EV' hoặc 'Battery'")
            @RequestParam(required = false) String type,
            @Parameter(description = "Số trang (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Số sản phẩm mỗi trang", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer size) {

        List<Product> products = productService.getAllProducts();

        // Lọc chỉ sản phẩm đang bán
        products = products.stream()
                .filter(p -> p.getStatus() == ProductStatus.DANG_BAN)
                .collect(Collectors.toList());

        // Lọc theo type nếu có
        if (type != null && !type.isEmpty()) {
            products = products.stream()
                    .filter(p -> type.equals(p.getType()))
                    .collect(Collectors.toList());
        }

        // Pagination: 10 sản phẩm/trang
        int totalProducts = products.size();
        int totalPages = (int) Math.ceil((double) totalProducts / size);
        int start = page * size;
        int end = Math.min(start + size, totalProducts);

        List<Product> paginatedProducts = (start < totalProducts)
            ? products.subList(start, end)
            : List.of();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("products", paginatedProducts);
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("totalProducts", totalProducts);
        response.put("totalPages", totalPages);

        return ResponseEntity.ok(response);
    }

    //FE-02: Tìm kiếm và lọc sản phẩm theo nhiều tiêu chí
    //Query params: brand, year, minPrice, maxPrice, condition, type
    @Operation(
            summary = "Tìm kiếm và lọc sản phẩm",
            description = "Tìm kiếm sản phẩm theo nhiều tiêu chí như tên, thương hiệu, năm sản xuất, giá, tình trạng, loại sản phẩm."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách sản phẩm tìm được"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/products/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String type) {

        List<Product> products = productService.getAllProducts();

        // Lọc chỉ sản phẩm đang bán
        products = products.stream()
                .filter(p -> p.getStatus() == ProductStatus.DANG_BAN)
                .collect(Collectors.toList());

        // Lọc theo keyword (tìm trong tên sản phẩm hoặc description)
        if (keyword != null && !keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getProductname().toLowerCase().contains(lowerKeyword) ||
                               (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }

        // Lọc theo type
        if (type != null && !type.isEmpty()) {
            products = products.stream()
                    .filter(p -> type.equals(p.getType()))
                    .collect(Collectors.toList());
        }

        // Lọc theo brand
        if (brand != null && !brand.isEmpty()) {
            products = products.stream()
                    .filter(p -> {
                        if ("Car EV".equals(p.getType()) && p.getBrandcars() != null) {
                            return brand.equalsIgnoreCase(p.getBrandcars().getBrand());
                        } else if ("Battery".equals(p.getType()) && p.getBrandbattery() != null) {
                            return brand.equalsIgnoreCase(p.getBrandbattery().getBrand());
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // Lọc theo năm
        if (minYear != null || maxYear != null) {
            products = products.stream()
                    .filter(p -> {
                        int year = 0;
                        if (p.getBrandcars() != null) {
                            year = p.getBrandcars().getYear();
                        } else if (p.getBrandbattery() != null) {
                            year = p.getBrandbattery().getYear();
                        }

                        if (minYear != null && year < minYear) return false;
                        if (maxYear != null && year > maxYear) return false;
                        return year > 0;
                    })
                    .collect(Collectors.toList());
        }

        // Lọc theo giá
        if (minPrice != null) {
            products = products.stream()
                    .filter(p -> p.getCost() >= minPrice)
                    .collect(Collectors.toList());
        }
        if (maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getCost() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Lọc theo condition (chỉ cho pin)
        if (condition != null && !condition.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getBrandbattery() != null &&
                               condition.equalsIgnoreCase(p.getBrandbattery().getCondition()))
                    .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("products", products);
        response.put("totalResults", products.size());
        response.put("filters", Map.of(
                "keyword", keyword != null ? keyword : "",
                "brand", brand != null ? brand : "",
                "type", type != null ? type : "",
                "priceRange", (minPrice != null || maxPrice != null) ?
                        Map.of("min", minPrice != null ? minPrice : 0, "max", maxPrice != null ? maxPrice : 0) : null
        ));

        return ResponseEntity.ok(response);
    }

    //FE-03: Xem chi tiết sản phẩm
    //Tự động tăng viewCount khi xem chi tiết
    @Operation(
            summary = "Xem chi tiết sản phẩm",
            description = "Lấy thông tin chi tiết của một sản phẩm bao gồm thông tin cơ bản, người bán, đánh giá."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin chi tiết sản phẩm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getProductDetail(@PathVariable Long productId) {
        try {
            Product product = productService.getProductById(productId);

            // Tăng view count
            product.setViewCount(product.getViewCount() + 1);
            productService.updateProduct(productId, product);

            // Lấy thông tin seller
            User seller = product.getUsers();
            Map<String, Object> sellerInfo = Map.of(
                    "sellerId", seller.getUserid(),
                    "username", seller.getUsername(),
                    "displayName", seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername(),
                    "email", seller.getEmail(),
                    "phone", seller.getPhone() != null ? seller.getPhone() : "N/A"
            );

            // Lấy feedbacks/ratings
            List<Map<String, Object>> feedbacks = product.getFeedbacks().stream()
                    .map(f -> {
                        Map<String, Object> fb = new HashMap<>();
                        fb.put("rating", f.getRating());
                        fb.put("comment", f.getComment() != null ? f.getComment() : "");
                        fb.put("createdAt", f.getCreated_at());
                        return fb;
                    })
                    .collect(Collectors.toList());

            double avgRating = product.getFeedbacks().isEmpty() ? 0.0 :
                    product.getFeedbacks().stream()
                            .mapToInt(Feedback::getRating)
                            .average()
                            .orElse(0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("product", product);
            response.put("seller", sellerInfo);
            response.put("feedbacks", feedbacks);
            response.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
            response.put("totalReviews", product.getFeedbacks().size());
            response.put("viewCount", product.getViewCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Không tìm thấy sản phẩm: " + e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //Xem thông tin người bán và các sản phẩm họ đang đăng
    @Operation(
            summary = "Xem thông tin người bán",
            description = "Lấy thông tin chi tiết của người bán bao gồm thông tin cá nhân và danh sách sản phẩm đang bán."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin người bán và sản phẩm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người bán"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<Map<String, Object>> getSellerInfo(@PathVariable Long sellerId) {
        try {
            User seller = userService.getUserById(sellerId);

            // Lấy tất cả sản phẩm đang bán của seller
            List<Product> sellerProducts = productService.getProductsBySeller(sellerId);
            sellerProducts = sellerProducts.stream()
                    .filter(p -> p.getStatus() == ProductStatus.DANG_BAN)
                    .collect(Collectors.toList());

            // Tính rating trung bình của seller
            double avgRating = sellerProducts.stream()
                    .flatMap(p -> p.getFeedbacks().stream())
                    .mapToInt(f -> f.getRating())
                    .average()
                    .orElse(0.0);

            int totalReviews = (int) sellerProducts.stream()
                    .flatMap(p -> p.getFeedbacks().stream())
                    .count();

            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("sellerId", seller.getUserid());
            sellerInfo.put("username", seller.getUsername());
            sellerInfo.put("displayName", seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername());
            sellerInfo.put("email", seller.getEmail());
            sellerInfo.put("phone", seller.getPhone() != null ? seller.getPhone() : "N/A");
            sellerInfo.put("memberSince", seller.getCreated_at());
            sellerInfo.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
            sellerInfo.put("totalReviews", totalReviews);
            sellerInfo.put("totalProducts", sellerProducts.size());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("seller", sellerInfo);
            response.put("products", sellerProducts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Không tìm thấy người bán: " + e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //FE-06: Endpoint để kiểm tra xem action có yêu cầu đăng nhập không
    //FE gọi API này trước khi thực hiện action mua hoặc chat
    @Operation(
            summary = "Kiểm tra yêu cầu đăng nhập",
            description = "Kiểm tra xem action mua hoặc chat có yêu cầu đăng nhập hay không."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin yêu cầu đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuthRequired(
            @RequestParam String action) { // "buy" hoặc "chat"

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("action", action);
        response.put("requiresAuth", true);
        response.put("message", "Bạn cần đăng nhập để " +
                ("buy".equals(action) ? "mua sản phẩm" : "chat với người bán"));
        response.put("loginUrl", "/api/auth/login");
        response.put("registerUrl", "/api/auth/register");

        return ResponseEntity.ok(response);
    }

    //Xem danh sách gói dịch vụ theo loại (dành cho guest và seller)
    @Operation(
            summary = "Xem danh sách gói dịch vụ",
            description = "Lấy danh sách tất cả gói dịch vụ hoặc lọc theo loại gói (CAR/BATTERY)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách gói dịch vụ"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/package-services")
    public ResponseEntity<Map<String, Object>> getAllPackageServices(
            @RequestParam(required = false) String packageType) {

        List<PackageService> packages;

        // Lọc theo packageType nếu có ("CAR" hoặc "BATTERY")
        if (packageType != null && !packageType.isEmpty()) {
            packages = packageServiceService.getPackagesByType(packageType);
        } else {
            packages = packageServiceService.getAllPackages();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("packages", packages);
        response.put("totalPackages", packages.size());

        if (packageType != null) {
            response.put("packageType", packageType);
        }

        return ResponseEntity.ok(response);
    }
}
