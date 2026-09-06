package com.zosh.service;

import java.util.List;

import com.zosh.dto.deal.DealRequest;
import com.zosh.dto.deal.DealResponse;
import com.zosh.model.Deal;

public interface DealService {

    // ─── Legacy methods (preserving existing admin & home category callers) ───
    List<Deal> getDeals();
    Deal createDeal(Deal deal);
    Deal updateDeal(Deal deal, Long id);
    void deleteDeal(Long id);

    // ─── Enterprise Deal Management ──────────────────────────────────────────
    DealResponse createDealFromRequest(DealRequest req, Long sellerId, boolean isAdmin);
    DealResponse updateDealFromRequest(Long id, DealRequest req, Long sellerId, boolean isAdmin);
    void deleteDealWithAuth(Long id, Long sellerId, boolean isAdmin);
    DealResponse toggleDealStatus(Long id, Long sellerId, boolean isAdmin);
    List<DealResponse> getSellerDeals(Long sellerId);
    List<DealResponse> getAllDealsAdmin();
    DealResponse getDealById(Long id, Long sellerId, boolean isAdmin);
    List<DealResponse> getActivePublicDeals();
}
