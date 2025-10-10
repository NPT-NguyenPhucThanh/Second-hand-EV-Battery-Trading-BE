package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.PackageService;

import java.util.List;

public interface PackageServiceService {
    List<PackageService> getAllPackages();
    PackageService getPackageById(Long id);
}
