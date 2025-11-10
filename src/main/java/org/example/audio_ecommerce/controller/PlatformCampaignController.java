package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/**
 * 🎯 Controller quản lý chương trình khuyến mãi trên toàn hệ thống.
 * Hỗ trợ 2 loại chiến dịch:
 * - MEGA_SALE → Giảm giá toàn sàn, áp dụng cùng lúc.
 * - FAST_SALE → Flash Sale nhiều khung giờ (slot).
 * <p>
 * Các trạng thái (VoucherStatus):
 * • DRAFT → Mới tạo, chờ duyệt hoặc chưa kích hoạt.
 * • APPROVE → Đã được admin duyệt, chờ đến thời gian để bật.
 * • ACTIVE → Đang hoạt động trong khung giờ hoặc thời gian diễn ra.
 * • EXPIRED → Đã hết hạn.
 * • DISABLED → Đã bị vô hiệu hóa tạm thời.
 * <p>
 * Các loại giảm giá (VoucherType):
 * • FIXED → Giảm theo số tiền cố định.
 * • PERCENT → Giảm theo phần trăm (%).
 * • SHIPPING → Miễn phí vận chuyển.
 */
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Platform Campaign", description = "🎯 Quản lý chương trình khuyến mãi Mega Sale / Flash Sale (Admin + Store)")
public class PlatformCampaignController {

    private final PlatformCampaignService service;
    private final PlatformCampaignService platformCampaignService;

    // =============================================================
    // ✅ 1) ADMIN TẠO CHIẾN DỊCH (MEGA_SALE / FAST_SALE)
    // =============================================================
    @PostMapping
    @Operation(summary = "🧩 Tạo campaign hợp nhất (Admin)",
            description = """
                    - **Admin** tạo mới chiến dịch Mega Sale hoặc Flash Sale.
                    - Nếu là `FAST_SALE`, bắt buộc gửi danh sách `flashSlots` (mở/đóng theo giờ).
                    - Trạng thái mặc định khi tạo mới: `DRAFT`.
                    - Badge và icon mặc định sẽ được hệ thống gán theo loại chiến dịch.
                    """)
    public ResponseEntity<BaseResponse> createUnified(@RequestBody CreateOrUpdateCampaignRequest req) {
        return service.createCampaignUnified(req);
    }

    // =============================================================
    // ✅ 2) STORE THAM GIA CHIẾN DỊCH (ĐĂNG KÝ SẢN PHẨM)
    // =============================================================
    @PostMapping("/{campaignId}/join")
    @Operation(summary = "🏪 Store tham gia chiến dịch (Đăng ký sản phẩm)",
            description = """
                    - **Store** gửi danh sách sản phẩm muốn tham gia campaign.
                    - Nếu là `FAST_SALE`, **bắt buộc** chỉ định `slotId` cho từng sản phẩm.
                    - Sản phẩm được lưu ở trạng thái `DRAFT` (chờ duyệt).
                    - Hệ thống sẽ tính toán `discountedPrice` dựa vào `VoucherType`:
                        • FIXED → Giảm số tiền cố định.
                        • PERCENT → Giảm theo phần trăm.
                        • SHIPPING → Miễn phí vận chuyển.
                    
                    ⚠️ Điều kiện:
                    - Store phải ở trạng thái `ACTIVE`.
                    - Product thuộc về chính store đó và đã được cập nhật ≥ 7 ngày trước.
                    """)
    public ResponseEntity<BaseResponse> join(@PathVariable UUID campaignId,
                                             @RequestBody CampaignProductRegisterRequest req) {
        return service.joinCampaign(campaignId, req);
    }

    // =============================================================
    // ✅ 3) LẤY DANH SÁCH CHIẾN DỊCH FLASH SALE (FAST_SALE)
    // =============================================================
    @GetMapping("/fast-sale")
    @Operation(summary = "⚡ Lấy danh sách campaign FAST_SALE (Flash Sale)",
            description = """
                    - Dành cho FE hiển thị danh sách Flash Sale cùng các khung giờ (slot).
                    - Có thể lọc theo:
                        • `status` = DRAFT / ACTIVE / EXPIRED / DISABLED / APPROVE
                        • `start`, `end` = giới hạn theo thời gian.
                    - Mỗi campaign trả về danh sách slot gồm:
                        → slotId, openTime, closeTime, status.
                    """)
    public ResponseEntity<BaseResponse> fastSaleList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.getFastSaleCampaigns(status, start, end);
    }

    // =============================================================
    // ✅ 4) LẤY SẢN PHẨM TRONG SLOT CỤ THỂ (FAST_SALE)
    // =============================================================
    @GetMapping("/{campaignId}/slots/{slotId}/products")
    @Operation(summary = "🕒 Lấy danh sách sản phẩm theo khung giờ Flash Sale (FAST_SALE)",
            description = """
                    - Dành cho người dùng FE hiển thị sản phẩm trong từng slot cụ thể.
                    - Tham số `timeFilter`:
                        • `UPCOMING` → sắp diễn ra  
                        • `ONGOING` → đang diễn ra  
                        • `EXPIRED` → đã kết thúc
                    - Mỗi sản phẩm có trạng thái riêng (DRAFT / APPROVE / ACTIVE / EXPIRED).
                    - Kèm thông tin giảm giá (VoucherType + giá trị giảm).
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
    @Operation(summary = "📦 Lấy danh sách tất cả campaign (Mega + Flash)",
            description = """
                    - Hiển thị danh sách toàn bộ chiến dịch cho **Admin** hoặc **Client**.
                    - Có thể lọc theo:
                        • `type` = MEGA_SALE / FAST_SALE  
                        • `status` = DRAFT / ACTIVE / EXPIRED / DISABLED / APPROVE  
                        • `start` & `end` = thời gian bắt đầu / kết thúc.
                    - Nếu là `FAST_SALE` → trả thêm danh sách slot (flashSlots).
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
    // ✅ 6) ADMIN XEM DANH SÁCH SẢN PHẨM TRONG CHIẾN DỊCH
    // =============================================================
    @GetMapping("/{campaignId}/products")
    @Operation(summary = "👁️ Admin xem danh sách sản phẩm trong campaign",
            description = """
                    - Xem toàn bộ sản phẩm tham gia 1 chiến dịch cụ thể.
                    - Có thể lọc:
                        • `storeId` (lọc theo cửa hàng)
                        • `status` = DRAFT / APPROVE / ACTIVE / EXPIRED / DISABLED
                        • `from` - `to` = khoảng thời gian đăng ký.
                    - Trả về:
                        • campaignProductId (id bảng trung gian)
                        • productName, storeName, voucher info, status.
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
    // ✅ 7) ADMIN DUYỆT SẢN PHẨM (DRAFT → APPROVE)
    // =============================================================
    @PutMapping("/{campaignId}/approve-products")
    @Operation(summary = "✅ Duyệt sản phẩm trong campaign (Admin)",
            description = """
                    - Duyệt hàng loạt sản phẩm từ `DRAFT` → `APPROVE`.
                    - Sản phẩm chỉ `ACTIVE` khi campaign hoặc slot bắt đầu (do scheduler tự bật).
                    - Request body: danh sách UUID của sản phẩm (`campaignProductId`).
                    """)
    public ResponseEntity<BaseResponse> approveCampaignProducts(
            @PathVariable UUID campaignId,
            @RequestBody List<UUID> productIds
    ) {
        return service.approveCampaignProducts(campaignId, productIds);
    }

    // =============================================================
    // ✅ 8) ADMIN ĐỔI TRẠNG THÁI SẢN PHẨM (ACTIVE / DISABLED / EXPIRED / DRAFT)
    // =============================================================
    @PostMapping("/{campaignId}/products/change-status")
    @Operation(summary = "🧭 Admin đổi trạng thái sản phẩm trong campaign",
            description = """
                    - Dùng để thay đổi trạng thái sản phẩm:
                        • DRAFT → APPROVE / ACTIVE  
                        • ACTIVE → DISABLED / EXPIRED
                        • DISABLED → DRAFT / ACTIVE
                    - Khi chuyển sang `ACTIVE`, hệ thống tự set `approved = true`.
                    - Request body: danh sách UUID sản phẩm (`campaignProductId`).
                    """)
    public ResponseEntity<BaseResponse> updateCampaignProductStatus(
            @PathVariable UUID campaignId,
            @RequestParam String newStatus,
            @RequestBody List<UUID> productIds
    ) {
        return service.updateCampaignProductStatus(campaignId, newStatus, productIds);
    }

    // =============================================================
    // ✅ 9) ADMIN CẬP NHẬT CHIẾN DỊCH (THÔNG TIN + SLOT)
    // =============================================================
    @PutMapping("/{campaignId}")
    @Operation(summary = "🛠️ Cập nhật thông tin campaign (Admin)",
            description = """
                    - Cho phép cập nhật thông tin:
                        • name, description, badge, thời gian, allowRegistration, approvalRule.
                        • Nếu là `FAST_SALE`, có thể gửi danh sách flashSlots:
                            - Có `id` → cập nhật slot cũ.
                            - Không có `id` → tạo slot mới.
                    - Nếu đổi status:
                        • `DISABLED` → khóa toàn bộ slot & sản phẩm.
                        • `ACTIVE` → mở lại slot & sản phẩm tương ứng.
                    """)
    public ResponseEntity<BaseResponse<CampaignResponse>> updateCampaign(
            @PathVariable UUID campaignId,
            @RequestBody UpdateCampaignRequest request
    ) {
        return service.updateCampaign(campaignId, request);
    }

    // =============================================================
    // ✅ 10) OVERVIEW — TỔNG HỢP SẢN PHẨM + CHIẾN DỊCH (CHO FE)
    // =============================================================
    @GetMapping("/overview")
    @Operation(summary = "📊 Lấy tổng quan sản phẩm theo chiến dịch (Mega + Flash)",
            description = """
                        - Dành cho FE hiển thị danh sách sản phẩm khuyến mãi.
                        - Có thể lọc theo:
                            • type = MEGA_SALE / FAST_SALE
                            • status = DRAFT / APPROVE / ACTIVE / EXPIRED / DISABLED
                            • storeId = lọc theo cửa hàng
                            • campaignId = lọc theo chiến dịch cụ thể
                        - Hỗ trợ phân trang (page, size).
                    """)
    public ResponseEntity<BaseResponse> getCampaignProductOverviewFiltered(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID campaignId, // ✅ thêm campaignId
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getCampaignProductOverviewFiltered(type, status, storeId, campaignId, page, size);
    }


    @PatchMapping("/{campaignId}/status")
    @Operation(
            summary = "🔄 Admin thay đổi trạng thái Campaign",
            description = """
                        Trạng thái campaign flow chuẩn:
                    
                        • DRAFT → ONOPEN   (Admin mở đăng ký store join)
                        • ONOPEN → ACTIVE  (Scheduler tự bật khi tới startTime)
                        • ACTIVE → EXPIRED (Scheduler tự tắt khi qua endTime)
                        • DISABLED         (Admin khoá campaign bất cứ lúc nào)
                    
                        ❗ FE cần nhớ:
                        - FE chỉ gọi API để chuyển: DRAFT → ONOPEN hoặc DISABLED
                        - FE KHÔNG được chuyển → ACTIVE thủ công (bị chặn BE)
                        - FE KHÔNG được set EXPIRED (scheduler tự set)
                    
                        Đây là chuẩn marketplace real (Shopee / TTS / Lazada)
                    """
    )
@Parameter(name = "status", description = "ONOPEN hoặc DISABLED")
@PutMapping("/{campaignId}/status")
public ResponseEntity<BaseResponse> updateCampaignStatus(
        @PathVariable UUID campaignId,
        @RequestParam String status
) {
    return platformCampaignService.updateCampaignStatus(campaignId, status);
}


@GetMapping("/joined-campaigns")
@Operation(
        summary = "Lấy danh sách campaign mà store đã join",
        description = """
        Filter danh sách các campaign mà store đã tham gia.

        - campaignStatus: ONOPEN | ACTIVE | EXPIRED
        - storeApproved : true | false | null
        """
)
public ResponseEntity<List<CampaignResponse>> getJoinedCampaigns(
        @RequestParam UUID storeId,
        @RequestParam(required = false) String campaignStatus,
        @RequestParam(required = false) Boolean storeApproved
) {
    return platformCampaignService.getJoinedCampaignsByCampaignStatus(storeId, campaignStatus, storeApproved);
}



}
