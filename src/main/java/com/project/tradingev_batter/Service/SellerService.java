package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.UserPackage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SellerService {
    // Quản lý gói đăng bán
    UserPackage purchasePackage(Long sellerId, Long packageId);
    UserPackage getCurrentPackage(Long sellerId);
    boolean isPackageExpired(UserPackage userPackage);
    UserPackage renewPackage(Long sellerId, Long packageId);
    
    // Kiểm tra có thể đăng sản phẩm không
    boolean canPostCar(Long sellerId);
    boolean canPostBattery(Long sellerId);
    
    // Đăng bán sản phẩm
    Product createCarProduct(Long sellerId, String productname, String description, double cost,
                            String licensePlate, String model, String specs, String brand, int year,
                            MultipartFile[] images);
    
    Product createBatteryProduct(Long sellerId, String productname, String description, double cost,
                                double capacity, double voltage, String brand, String condition,
                                String pickupAddress, MultipartFile[] images);
    
    // Quản lý sản phẩm
    List<Product> getSellerCarProducts(Long sellerId);
    Product updateBatteryProduct(Long sellerId, Long productId, Product updatedProduct);
    void deleteBatteryProduct(Long sellerId, Long productId);
    
    // Theo dõi đơn hàng
    List<Orders> getSellerCarOrders(Long sellerId);
    List<Orders> getSellerBatteryOrders(Long sellerId);
    
    // Thống kê & Doanh thu
    Map<String, Object> getSellerStatistics(Long sellerId);
    Map<String, Object> getRevenueDetails(Long sellerId);
}
