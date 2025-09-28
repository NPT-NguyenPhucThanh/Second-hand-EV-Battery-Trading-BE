package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.tradingev_batter.Entity.Contracts;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractsRepository extends JpaRepository<Contracts, Long> {
}