package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.PackageService;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageServiceRepository extends JpaRepository<PackageService, Long> {
    // Tìm tất cả gói theo loại (CAR hoặc BATTERY)
    List<PackageService> findByPackageType(String packageType);

    // Tìm gói theo tên và loại (để tránh duplicate khi seed)
    Optional<PackageService> findByNameAndPackageType(String name, String packageType);
}
