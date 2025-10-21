package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.PackageService;

import java.util.List;

@Repository
public interface PackageServiceRepository extends JpaRepository<PackageService, Long> {
    // Tìm tất cả gói theo loại (CAR hoặc BATTERY)
    List<PackageService> findByPackageType(String packageType);
}
