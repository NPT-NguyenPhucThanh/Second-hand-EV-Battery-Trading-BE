package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    List<Product> findByTypeAndInWarehouse(String type, boolean inWarehouse);
    List<Product> findByTypeAndStatusAndInWarehouse(String type, ProductStatus status, boolean inWarehouse);
    List<Product> findByUsers(User user);
    List<Product> findByUsers_Userid(Long userId);

    // Tìm product theo tên và seller (seeds)
    Optional<Product> findByProductnameAndUsers(String productname, User user);

    @Query("SELECT p FROM Product p LEFT JOIN p.brandcars bc LEFT JOIN p.brandbattery bb WHERE (:type IS NULL OR p.type = :type) " +
            "AND (:brand IS NULL OR p.model LIKE %:brand%) AND (:yearMin IS NULL OR (bc.year >= :yearMin OR bb.year >= :yearMin)) " +
            "AND (:yearMax IS NULL OR (bc.year <= :yearMax OR bb.year <= :yearMax)) " +
            "AND (:capacityMin IS NULL OR (bc.capacity >= :capacityMin OR bb.capacity >= :capacityMin)) " +
            "AND (:capacityMax IS NULL OR (bc.capacity <= :capacityMax OR bb.capacity <= :capacityMax))" +
            "AND (:status IS NULL OR p.status = :status) AND (:priceMin IS NULL OR p.cost >= :priceMin) " +
            "AND (:priceMax IS NULL OR p.cost <= :priceMax)")
    List<Product> findByFilters(@Param("type") String type, @Param("brand") String brand, @Param("yearMin") Integer yearMin,
                                @Param("yearMax") Integer yearMax, @Param("capacityMin") Double capacityMin,
                                @Param("capacityMax") Double capacityMax, @Param("status") String status, @Param("priceMin") Double priceMin,
                                @Param("priceMax") Double priceMax);
}
