package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.product_img;

@Repository
public interface ProductImgRepository extends JpaRepository<product_img,Long> {
}
