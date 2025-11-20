package com.project.tradingev_batter.Service;

import java.util.List;

import com.project.tradingev_batter.Entity.Contracts;

public interface ContractService {

    // Tạo contract sau khi kiểm định PASS
    Contracts createContractAfterInspection(Long productId, Long sellerId);

    // Lấy danh sách contract pending của seller
    List<Contracts> getPendingContracts(Long sellerId);

    // Seller ký contract
    Contracts signContract(Long contractId, Long sellerId);

    // Xử lý DocuSeal callback
    Contracts handleDocuSealCallback(String submissionId, String status);

    // Lấy contract theo orderId
    Contracts getContractByOrderId(Long orderId);

    // Lấy tất cả contract của seller
    List<Contracts> getSellerContracts(Long sellerId);

    List<Contracts> getBuyerContracts(Long buyerId);
}
