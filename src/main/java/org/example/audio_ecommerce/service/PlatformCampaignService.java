// org.example.audio_ecommerce.service.PlatformCampaignService
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.request.UpdateCampaignRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CampaignResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlatformCampaignService {

    // 1) Tạo campaign hợp nhất (MEGA_SALE / FAST_SALE)
    ResponseEntity<BaseResponse> createCampaignUnified(CreateOrUpdateCampaignRequest req);

     // Cập nhật campaign (bao gồm cập nhật slot)
    ResponseEntity<BaseResponse<CampaignResponse>> updateCampaign(UUID campaignId, UpdateCampaignRequest req);


    // 2) Store tham gia campaign (thêm sản phẩm; nếu FAST_SALE phải kèm slotId)
    ResponseEntity<BaseResponse> joinCampaign(UUID campaignId, CampaignProductRegisterRequest req);

    // 3) Get danh sách campaign FAST_SALE (kèm slots) theo bộ lọc
    ResponseEntity<BaseResponse> getFastSaleCampaigns(String status, LocalDateTime start, LocalDateTime end);

    // 4) Get sản phẩm theo khung giờ (slot) + filter thời gian: EXPIRED / ONGOING / UPCOMING
    ResponseEntity<BaseResponse> getSlotProducts(UUID campaignId, UUID slotId, String timeFilter);

    void tickAllCampaigns();

    // 6) Lấy tất cả campaign (lọc theo type, status, start, end)
    ResponseEntity<BaseResponse> getAllCampaigns(String type, String status, LocalDateTime start, LocalDateTime end);

    // 1️⃣ Lấy danh sách sản phẩm trong campaign (lọc theo store, thời gian, trạng thái)
ResponseEntity<BaseResponse> getCampaignProducts(
        UUID campaignId,
        UUID storeId,
        String status,
        LocalDateTime from,
        LocalDateTime to
);

// 2️⃣ Admin duyệt sản phẩm
ResponseEntity<BaseResponse> approveCampaignProducts(UUID campaignId, List<UUID> productIds);

ResponseEntity<BaseResponse> updateCampaignProductStatus(UUID campaignId, String newStatus, List<UUID> productIds);

ResponseEntity<BaseResponse> getCampaignProductOverviewFiltered(
        String type,
        String status,
        UUID storeId,
        UUID campaignId,   // ✅ thêm filter theo campaignId
        int page,
        int size
);

}
