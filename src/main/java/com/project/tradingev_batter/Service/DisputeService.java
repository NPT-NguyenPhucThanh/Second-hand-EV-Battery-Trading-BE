package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Dispute;

import java.util.List;

public interface DisputeService {
    Dispute createDispute(Long buyerId, Long orderId, String description, String reasonType);
    List<Dispute> getDisputesByBuyer(Long buyerId);
    List<Dispute> getAllDisputes(); // For manager
    Dispute getDisputeById(Long disputeId);
    Dispute resolveDispute(Long disputeId, String decision, String managerNote);
}
