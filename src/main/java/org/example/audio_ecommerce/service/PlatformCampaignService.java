// org.example.audio_ecommerce.service.PlatformCampaignService
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PlatformCampaignService {

    // 1) Tạo campaign hợp nhất (MEGA_SALE / FAST_SALE)
    ResponseEntity<BaseResponse> createCampaignUnified(CreateOrUpdateCampaignRequest req);

    // 2) Store tham gia campaign (thêm sản phẩm; nếu FAST_SALE phải kèm slotId)
    ResponseEntity<BaseResponse> joinCampaign(UUID campaignId, CampaignProductRegisterRequest req);

    // 3) Get danh sách campaign FAST_SALE (kèm slots) theo bộ lọc
    ResponseEntity<BaseResponse> getFastSaleCampaigns(String status, LocalDateTime start, LocalDateTime end);

    // 4) Get sản phẩm theo khung giờ (slot) + filter thời gian: EXPIRED / ONGOING / UPCOMING
    ResponseEntity<BaseResponse> getSlotProducts(UUID campaignId, UUID slotId, String timeFilter);

    // 5) Scheduler: bật/tắt slot theo thời gian & cập nhật trạng thái sản phẩm
    void tickFlashSlots();

    // 6) Lấy tất cả campaign (lọc theo type, status, start, end)
    ResponseEntity<BaseResponse> getAllCampaigns(String type, String status, LocalDateTime start, LocalDateTime end);
}
