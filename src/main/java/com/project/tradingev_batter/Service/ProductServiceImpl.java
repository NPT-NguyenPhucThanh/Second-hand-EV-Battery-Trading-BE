package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.ProductStatus;
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

    public ProductServiceImpl(ProductRepository productRepository, UserService userService) {
        this.userService = userService;
        this.productRepository = productRepository;
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
                .filter(p -> "DA_DUYET".equals(p.getStatus()) || "DANG_BAN".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        product.setCreatedat(new Date());
        product.setStatus("CHO_DUYET"); // Mặc định chờ duyệt
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
        productRepository.deleteById(id);
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
}
