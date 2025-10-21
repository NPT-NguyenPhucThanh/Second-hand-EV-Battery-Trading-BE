package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.product_img;

import java.util.List;

@Repository
public interface ProductImgRepository extends JpaRepository<product_img,Long> {
    // Tìm tất cả images của một product để check trùng lặp khi seed
    List<product_img> findByProducts(Product product);
}
