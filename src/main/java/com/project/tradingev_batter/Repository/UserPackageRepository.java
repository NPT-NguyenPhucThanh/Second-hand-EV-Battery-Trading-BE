package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.UserPackage;

import java.util.Date;
import java.util.List;

@Repository
public interface UserPackageRepository extends JpaRepository<UserPackage, Long> {
    List<UserPackage> findByUser_UseridOrderByExpiryDateDesc(Long userId);

    // Tìm gói hiện tại của user theo loại package (CAR hoặc BATTERY)
    @Query("SELECT up FROM UserPackage up WHERE up.user.userid = :userId " +
           "AND up.packageService.packageType = :packageType " +
           "AND up.expiryDate > :currentDate " +
           "ORDER BY up.expiryDate DESC")
    List<UserPackage> findActivePackageByUserAndType(
        @Param("userId") Long userId,
        @Param("packageType") String packageType,
        @Param("currentDate") Date currentDate
    );

    // Tìm tất cả gói còn hiệu lực của user
    @Query("SELECT up FROM UserPackage up WHERE up.user.userid = :userId " +
           "AND up.expiryDate > :currentDate " +
           "ORDER BY up.expiryDate DESC")
    List<UserPackage> findActivePackagesByUser(
        @Param("userId") Long userId,
        @Param("currentDate") Date currentDate
    );

    // Check xem user đã mua package chưa (seeds)
    boolean existsByUserAndPackageService(User user, PackageService packageService);

    // Lấy tất cả user đang có gói package đang hoạt động
    @Query("SELECT up FROM UserPackage up WHERE up.expiryDate > :currentDate " +
           "ORDER BY up.expiryDate DESC")
    List<UserPackage> findAllActivePackages(@Param("currentDate") Date currentDate);

    // Lấy user package theo loại package đang hoạt động
    @Query("SELECT up FROM UserPackage up WHERE up.packageService.packageType = :packageType " +
           "AND up.expiryDate > :currentDate " +
           "ORDER BY up.expiryDate DESC")
    List<UserPackage> findActivePackagesByType(
        @Param("packageType") String packageType,
        @Param("currentDate") Date currentDate
    );
}
