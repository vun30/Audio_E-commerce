package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.PlatformCampaignService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class PlatformCampaignController {
    private final PlatformCampaignService service;

    @PostMapping
    public ResponseEntity<BaseResponse> createUnified(@RequestBody CreateOrUpdateCampaignRequest req) {
        return service.createCampaignUnified(req);
    }

    @PostMapping("/{campaignId}/join")
    public ResponseEntity<BaseResponse> join(@PathVariable UUID campaignId,
                                             @RequestBody CampaignProductRegisterRequest req) {
        return service.joinCampaign(campaignId, req);
    }

    @GetMapping("/fast-sale")
    public ResponseEntity<BaseResponse> fastSaleList(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) LocalDateTime start,
                                                     @RequestParam(required = false) LocalDateTime end) {
        return service.getFastSaleCampaigns(status, start, end);
    }

    @GetMapping("/{campaignId}/slots/{slotId}/products")
    public ResponseEntity<BaseResponse> slotProducts(@PathVariable UUID campaignId,
                                                     @PathVariable UUID slotId,
                                                     @RequestParam(required = false) String timeFilter) {
        return service.getSlotProducts(campaignId, slotId, timeFilter);
    }

    // Cron (ví dụ @Scheduled(fixedRate = 30000))
    @PostMapping("/fast-sale/tick")
    public void tickSlots() { service.tickFlashSlots(); }

    @GetMapping
@Operation(summary = "Lấy danh sách chiến dịch (Mega Sale + Fast Sale)")
public ResponseEntity<BaseResponse> getAllCampaigns(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
) {
    return service.getAllCampaigns(type, status, start, end);
}

}

