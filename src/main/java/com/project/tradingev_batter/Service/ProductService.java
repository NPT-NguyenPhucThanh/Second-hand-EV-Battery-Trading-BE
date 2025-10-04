package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    List<Product> searchAndFilterProducts(String type, String brand, Integer yearMin, Integer yearMax,
                                          Double capacityMin, Double capacityMax, String status, Double priceMin, Double priceMax);
    List<Product> getProductsBySeller(Long sellerId);
}
