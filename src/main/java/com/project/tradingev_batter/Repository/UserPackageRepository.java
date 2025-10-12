package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.UserPackage;

import java.util.List;

@Repository
public interface UserPackageRepository extends JpaRepository<UserPackage, Long> {
    List<UserPackage> findByUser_UseridOrderByExpiryDateDesc(Long userId);
}
