package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Product;

import java.util.List;
import java.util.Map;

public interface ProductService {
    List<Product> getAllProducts();
    
    // Guest features
    List<Product> getAllActiveProducts(); // Chỉ lấy sản phẩm đang active và đã duyệt
    
    Product getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    
    List<Product> searchAndFilterProducts(String type, String brand, Integer yearMin, Integer yearMax,
                                          Double capacityMin, Double capacityMax, String status, 
                                          Double priceMin, Double priceMax);
    
    List<Product> getProductsBySeller(Long sellerId);
    
    // Helper methods cho Guest
    List<String> getBrandsByType(String type);
    Map<String, Object> getPublicStatistics();

    // View tracking - Tăng viewCount khi user xem sản phẩm
    void incrementViewCount(Long productId);
}
