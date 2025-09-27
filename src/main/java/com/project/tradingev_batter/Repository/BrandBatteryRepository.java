package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Brandbattery;

@Repository
public interface BrandBatteryRepository extends JpaRepository<Brandbattery, Long> {
}