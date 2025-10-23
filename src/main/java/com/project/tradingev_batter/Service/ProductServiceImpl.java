package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.product_img;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final UserService userService;
    private final ImageUploadService imageUploadService;

    public ProductServiceImpl(ProductRepository productRepository, UserService userService, ImageUploadService imageUploadService) {
        this.userService = userService;
        this.productRepository = productRepository;
        this.imageUploadService = imageUploadService;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    //Guest chỉ xem được sản phẩm đã duyệt và đang bán
    @Override
    public List<Product> getAllActiveProducts() {
        List<Product> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(p -> ProductStatus.DA_DUYET.equals(p.getStatus()) || ProductStatus.DANG_BAN.equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

     //INTERNAL USE ONLY - Tạo product cơ bản không kiểm tra gói
     //CHÚ Ý!!!!!!!!!!!!!!: Method này chỉ dùng cho internal logic hoặc Manager/Admin
     //SELLER PHẢI DÙNG SellerService.createCarProduct() hoặc createBatteryProduct() để có kiểm tra gói đầy đủ
    @Override
    @Transactional
    public Product createProduct(Product product) {
        product.setCreatedat(new Date());
        product.setStatus(ProductStatus.CHO_DUYET); // Mặc định chờ duyệt
        product.setInWarehouse(false);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = getProductById(id);
        product.setProductname(updatedProduct.getProductname());
        product.setDescription(updatedProduct.getDescription());
        product.setCost(updatedProduct.getCost());
        product.setAmount(updatedProduct.getAmount());
        product.setStatus(updatedProduct.getStatus());
        product.setModel(updatedProduct.getModel());
        product.setType(updatedProduct.getType());
        product.setSpecs(updatedProduct.getSpecs());
        product.setUpdatedat(new Date());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        // Kiểm tra xem sản phẩm có đang trong đơn hàng nào không
        if (!product.getOrder_detail().isEmpty()) {
            throw new RuntimeException("Không thể xóa sản phẩm đang có trong đơn hàng");
        }

        // Xóa tất cả ảnh của sản phẩm từ Cloudinary trước khi xóa product
        if (product.getImgs() != null && !product.getImgs().isEmpty()) {
            for (product_img img : product.getImgs()) {
                try {
                    String imageUrl = img.getUrl();
                    if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
                        // Extract public_id từ Cloudinary URL
                        String publicId = extractPublicIdFromUrl(imageUrl);
                        if (publicId != null) {
                            imageUploadService.deleteImage(publicId);
                            System.out.println("Đã xóa ảnh từ Cloudinary: " + publicId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Không thể xóa ảnh từ Cloudinary: " + e.getMessage());
                    // Vẫn tiếp tục xóa product ngay cả khi xóa ảnh fail
                }
            }
        }

        productRepository.deleteById(id);
    }

    //Helper method: Extract public_id từ Cloudinary URL
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
                return null;
            }

            // Split by "/upload/"
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;

            // Lấy phần sau "/upload/v{version}/"
            String afterUpload = parts[1];

            // Remove version prefix (v1234567890/)
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");

            // Remove file extension
            int dotIndex = withoutVersion.lastIndexOf('.');
            if (dotIndex > 0) {
                withoutVersion = withoutVersion.substring(0, dotIndex);
            }

            return withoutVersion;
        } catch (Exception e) {
            System.err.println("Error extracting public_id: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Product> searchAndFilterProducts(String type, String brand, Integer yearMin, Integer yearMax,
                                                 Double capacityMin, Double capacityMax, String status, 
                                                 Double priceMin, Double priceMax) {
        return productRepository.findByFilters(type, brand, yearMin, yearMax, 
                capacityMin, capacityMax, status, priceMin, priceMax);
    }

    @Override
    public List<Product> getProductsBySeller(Long sellerId) {
        User seller = userService.getUserById(sellerId);
        return productRepository.findByUsers(seller);
    }

    //Lấy danh sách hãng theo loại sản phẩm
    @Override
    public List<String> getBrandsByType(String type) {
        List<Product> products = productRepository.findAll();
        Set<String> brands = new HashSet<>();
        
        for (Product p : products) {
            if (type == null || type.equals(p.getType())) {
                if (p.getBrandcars() != null && p.getBrandcars().getBrand() != null) {
                    brands.add(p.getBrandcars().getBrand());
                }
                if (p.getBrandbattery() != null && p.getBrandbattery().getBrand() != null) {
                    brands.add(p.getBrandbattery().getBrand());
                }
            }
        }
        
        return new ArrayList<>(brands);
    }

    //Lấy thống kê công khai cho Guest
    @Override
    public Map<String, Object> getPublicStatistics() {
        List<Product> activeProducts = getAllActiveProducts();
        
        long totalCars = activeProducts.stream()
                .filter(p -> "Car EV".equals(p.getType()))
                .count();
        
        long totalBatteries = activeProducts.stream()
                .filter(p -> "Battery".equals(p.getType()))
                .count();
        
        // Tính giá trung bình
        double avgPrice = activeProducts.stream()
                .mapToDouble(Product::getCost)
                .average()
                .orElse(0.0);
        
        // Tìm giá thấp nhất và cao nhất
        double minPrice = activeProducts.stream()
                .mapToDouble(Product::getCost)
                .min()
                .orElse(0.0);
        
        double maxPrice = activeProducts.stream()
                .mapToDouble(Product::getCost)
                .max()
                .orElse(0.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", activeProducts.size());
        stats.put("totalCars", totalCars);
        stats.put("totalBatteries", totalBatteries);
        stats.put("averagePrice", avgPrice);
        stats.put("priceRange", Map.of("min", minPrice, "max", maxPrice));
        stats.put("lastUpdated", new Date());
        
        return stats;
    }

    // VIEW TRACKING - Tăng viewCount khi user/guest xem sản phẩm
    // Mỗi lần gọi API này, viewCount sẽ tăng lên 1
    // Frontend nên gọi API này khi user vào trang chi tiết sản phẩm
    @Override
    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Tăng viewCount lên 1
        Integer currentViewCount = product.getViewCount();
        if (currentViewCount == null) {
            currentViewCount = 0;
        }
        product.setViewCount(currentViewCount + 1);

        // Cập nhật updatedat
        product.setUpdatedat(new Date());

        productRepository.save(product);
    }
}
