package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.PackageService;
import com.project.tradingev_batter.Repository.PackageServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackageServiceServiceImpl implements PackageServiceService {
    
    private final PackageServiceRepository packageServiceRepository;

    public PackageServiceServiceImpl(PackageServiceRepository packageServiceRepository) {
        this.packageServiceRepository = packageServiceRepository;
    }

    @Override
    public List<PackageService> getAllPackages() {
        return packageServiceRepository.findAll();
    }

    @Override
    public PackageService getPackageById(Long id) {
        return packageServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found"));
    }
}
