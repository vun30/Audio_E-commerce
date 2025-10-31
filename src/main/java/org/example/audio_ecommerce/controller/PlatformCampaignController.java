package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.request.UpdateCampaignRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CampaignResponse;
import org.example.audio_ecommerce.service.PlatformCampaignService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Platform Campaign", description = "Quản lý chương trình khuyến mãi Mega Sale / Flash Sale")
public class PlatformCampaignController {

    private final PlatformCampaignService service;

    // =============================================================
    // ✅ 1) TẠO CHIẾN DỊCH (MEGA_SALE / FAST_SALE)
    // =============================================================
    @PostMapping
    @Operation(summary = "Tạo campaign hợp nhất (Mega Sale / Fast Sale)",
            description = """
                    - Dùng cho admin để tạo chiến dịch mới.
                    - Nếu là **FAST_SALE**, cần kèm danh sách flashSlots.
                    - Trạng thái mặc định khi tạo mới: DRAFT.
                    """)
    public ResponseEntity<BaseResponse> createUnified(@RequestBody CreateOrUpdateCampaignRequest req) {
        return service.createCampaignUnified(req);
    }

    // =============================================================
    // ✅ 2) STORE JOIN CAMPAIGN (Đăng ký sản phẩm vào chiến dịch)
    // =============================================================
    @PostMapping("/{campaignId}/join")
    @Operation(summary = "Store tham gia chiến dịch (đăng ký sản phẩm)",
            description = """
                    - Dành cho chủ cửa hàng muốn tham gia campaign.
                    - Gửi danh sách productId + slotId (nếu là FAST_SALE).
                    """)
    public ResponseEntity<BaseResponse> join(@PathVariable UUID campaignId,
                                             @RequestBody CampaignProductRegisterRequest req) {
        return service.joinCampaign(campaignId, req);
    }

    // =============================================================
    // ✅ 3) LẤY DANH SÁCH FAST_SALE THEO BỘ LỌC
    // =============================================================
    @GetMapping("/fast-sale")
    @Operation(summary = "Lấy danh sách campaign FAST_SALE (Flash Sale)",
            description = """
                    - Có thể lọc theo status, startTime, endTime.
                    - Trả về danh sách campaign + slots.
                    """)
    public ResponseEntity<BaseResponse> fastSaleList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.getFastSaleCampaigns(status, start, end);
    }

    // =============================================================
    // ✅ 4) LẤY SẢN PHẨM THEO SLOT TRONG FAST_SALE
    // =============================================================
    @GetMapping("/{campaignId}/slots/{slotId}/products")
    @Operation(summary = "Lấy danh sách sản phẩm theo slot (FAST_SALE)",
            description = """
                    - Dùng cho người dùng xem sản phẩm trong khung giờ cụ thể.
                    - timeFilter = UPCOMING / ONGOING / EXPIRED.
                    """)
    public ResponseEntity<BaseResponse> slotProducts(
            @PathVariable UUID campaignId,
            @PathVariable UUID slotId,
            @RequestParam(required = false) String timeFilter) {
        return service.getSlotProducts(campaignId, slotId, timeFilter);
    }

    // =============================================================
    // ✅ 5) LẤY TOÀN BỘ CHIẾN DỊCH (ADMIN / CLIENT)
    // =============================================================
    @GetMapping
    @Operation(summary = "Lấy danh sách chiến dịch (Mega Sale + Fast Sale)",
            description = """
                    - Có thể lọc theo type (FAST_SALE / MEGA_SALE), status, hoặc khoảng thời gian.
                    """)
    public ResponseEntity<BaseResponse> getAllCampaigns(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return service.getAllCampaigns(type, status, start, end);
    }

    // =============================================================
    // ✅ 6) ADMIN XEM SẢN PHẨM TRONG CHIẾN DỊCH
    // =============================================================
    @GetMapping("/{campaignId}/products")
    @Operation(summary = "👁️ Admin xem danh sách sản phẩm tham gia campaign",
            description = """
                    - Lọc theo storeId, trạng thái (DRAFT / ACTIVE / EXPIRED).
                    - Lọc theo thời gian tham gia campaign (from/to).
                    """)
    public ResponseEntity<BaseResponse> getCampaignProducts(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return service.getCampaignProducts(campaignId, storeId, status, from, to);
    }

    // =============================================================
    // ✅ 7) ADMIN PHÊ DUYỆT SẢN PHẨM TRONG CHIẾN DỊCH
    // =============================================================
    @PutMapping("/{campaignId}/approve-products")
    @Operation(summary = "👑 Admin duyệt sản phẩm tham gia campaign",
            description = """
                    - Duyệt nhiều sản phẩm một lần (chuyển từ DRAFT → ACTIVE).
                    - Request body: danh sách productIds.
                    """)
    public ResponseEntity<BaseResponse> approveCampaignProducts(
            @PathVariable UUID campaignId,
            @RequestBody List<UUID> productIds
    ) {
        return service.approveCampaignProducts(campaignId, productIds);
    }

    // =============================================================
    // ✅ 8) ADMIN ĐỔI TRẠNG THÁI SẢN PHẨM TRONG CHIẾN DỊCH
    // =============================================================
    @PostMapping("/{campaignId}/products/change-status")
    @Operation(summary = "Admin đổi trạng thái sản phẩm trong campaign",
            description = """
                    - Thay đổi status của sản phẩm trong campaign (DRAFT, ACTIVE, DISABLED, EXPIRED...).
                    """)
    public ResponseEntity<BaseResponse> updateCampaignProductStatus(
            @PathVariable UUID campaignId,
            @RequestParam String newStatus,
            @RequestBody List<UUID> productIds
    ) {
        return service.updateCampaignProductStatus(campaignId, newStatus, productIds);
    }

    // =============================================================
    // ✅ 9) ADMIN CẬP NHẬT CHIẾN DỊCH (bao gồm cập nhật slot)
    // =============================================================
    @PutMapping("/{campaignId}")
    @Operation(summary = "🛠️ Cập nhật campaign (Admin)",
            description = """
                    - Cho phép cập nhật thông tin campaign (name, desc, badge...).
                    - Nếu là **FAST_SALE**, có thể gửi danh sách `flashSlots`:
                        * Có `id`: cập nhật slot cũ.
                        * Không có `id`: tạo slot mới.
                    - Khi cập nhật status → `DISABLED`: tất cả slot & sản phẩm bị disable.
                    - Khi bật lại → `ACTIVE`: slot & product được phục hồi tương ứng.
                    """)
    public ResponseEntity<BaseResponse<CampaignResponse>> updateCampaign(
            @PathVariable UUID campaignId,
            @RequestBody UpdateCampaignRequest request
    ) {
        return service.updateCampaign(campaignId, request);
    }

}
