package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;
import org.example.audio_ecommerce.service.WarrantyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Warranty", description = "API quản lý bảo hành (Kích hoạt • Tra cứu • Ticket • Review)")
@RestController
@RequestMapping("/api/warranties")
@RequiredArgsConstructor
public class WarrantyController {

    private final WarrantyService warrantyService;

    // ============================================================
    // ⚡ Kích hoạt bảo hành cho StoreOrder (thủ công / retry)
    // ============================================================
    @Operation(
            summary = "Kích hoạt bảo hành cho một StoreOrder",
            description = """
            • Dùng khi cần kích hoạt thủ công (retry) sau khi giao hàng thành công.  
            • Điều kiện: `CustomerOrder` **hoặc** `StoreOrder` đã là `DELIVERY_SUCCESS`.  
            • Hệ thống sẽ:
              - Duyệt mọi `StoreOrderItem`.
              - Bỏ qua `type=COMBO`.
              - Với `type=PRODUCT`, tạo **N Warranty** tương ứng `quantity` (mỗi sản phẩm 1 bản ghi để gán serial riêng).  
            • `durationMonths` đọc từ `Product.warrantyPeriod` (VD: "24 tháng"), không parse được ⇒ mặc định 12.  
            • `startDate = purchaseDate` (hiện lấy từ `CustomerOrder.createdAt`, bạn có thể đổi sang ngày giao thành công nếu có cột).  
            • Không trả danh sách Warranty; chỉ trả message “Activated”.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kích hoạt bảo hành thành công",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            )
    })
    @PostMapping("/activate/store-order/{storeOrderId}")
    public ResponseEntity<BaseResponse<Void>> activateForStoreOrder(
            @Parameter(description = "ID của StoreOrder cần kích hoạt bảo hành", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
            @PathVariable UUID storeOrderId
    ) {
        warrantyService.activateForStoreOrder(storeOrderId);
        return ResponseEntity.ok(BaseResponse.success("Activated"));
    }

    // ============================================================
    // 🔎 Tra cứu bảo hành (serial / orderId / phone|email)
    // ============================================================
    @Operation(
            summary = "Tra cứu bảo hành",
            description = """
            • Chọn **một trong ba** cách:  
              1) `serial` — số serial của thiết bị.  
              2) `orderId` — ID của `CustomerOrder` (trả tất cả bảo hành phát sinh từ đơn).  
              3) `q` — số điện thoại **hoặc** email của khách.  
            • Ưu tiên: nếu có `serial` → bỏ qua tham số khác.  
            • Kết quả: danh sách `WarrantyResponse`.  
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kết quả tra cứu bảo hành",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = WarrantyResponse.class))
                    )
            )
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<WarrantyResponse>>> search(
            @Parameter(description = "Serial number của sản phẩm", example = "SN-JBL-2025-000123")
            @RequestParam(required = false) String serial,

            @Parameter(description = "ID của CustomerOrder để truy tất cả bảo hành thuộc đơn", example = "4b1d6b8f-3a6a-4a9a-9f3d-2d0a5f3c1e11")
            @RequestParam(required = false) UUID orderId,

            @Parameter(name = "q", description = "Số điện thoại hoặc email của khách hàng", example = "0901123456 hoặc user@email.com")
            @RequestParam(required = false, name = "q") String phoneOrEmail
    ) {
        WarrantySearchRequest req = new WarrantySearchRequest();
        req.setSerial(serial);
        req.setOrderId(orderId);
        req.setPhoneOrEmail(phoneOrEmail);

        List<WarrantyResponse> result = warrantyService.search(req);
        return ResponseEntity.ok(BaseResponse.success("OK", result));
    }

    // ============================================================
    // 🔐 Gắn serial lần đầu cho Warranty
    // ============================================================
    @Operation(
            summary = "Gắn serial lần đầu",
            description = """
            • Dùng khi bản ghi Warranty **chưa có** `serialNumber`.  
            • Input:
              - `serialNumber` (bắt buộc)  
              - `note` (tùy chọn)  
            • BE chặn nếu serial đã tồn tại.  
            • Kết quả: `WarrantyResponse` sau cập nhật.  
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Thông tin serial cần gắn",
                    required = true,
                    content = @Content(schema = @Schema(implementation = WarrantyActivateSerialRequest.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gắn serial thành công",
                    content = @Content(schema = @Schema(implementation = WarrantyResponse.class))
            )
    })
    @PostMapping("/{id}/activate-serial")
    public ResponseEntity<BaseResponse<WarrantyResponse>> setSerial(
            @Parameter(description = "ID của Warranty cần gắn serial", example = "b2e3a9d4-0f4f-4b1f-8d71-4d5ab7f3c9af")
            @PathVariable UUID id,
            @RequestBody WarrantyActivateSerialRequest req
    ) {
        WarrantyResponse w = warrantyService.setSerialFirstTime(id, req.getSerialNumber(), req.getNote());
        return ResponseEntity.ok(BaseResponse.success("Serial set", w));
    }

    // ============================================================
    // 📝 Mở ticket bảo hành (tiếp nhận)
    // ============================================================
    @Operation(
            summary = "Mở ticket bảo hành (LogWarranty)",
            description = """
            • Tạo phiếu/ticket cho 1 Warranty ở trạng thái ban đầu `OPEN`.  
            • Body:
              - `problemDescription` — mô tả lỗi/tình trạng.  
              - `covered` — `null` → theo policy; `true/false` → cưỡng bức miễn/thu phí.  
              - `attachmentUrls` — danh sách URL ảnh/video/biên bản.  
            • Kết quả: `LogWarrantyResponse` (status = OPEN).  
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Thông tin mở ticket",
                    required = true,
                    content = @Content(schema = @Schema(implementation = WarrantyLogOpenRequest.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket đã được mở",
                    content = @Content(schema = @Schema(implementation = LogWarrantyResponse.class))
            )
    })
    @PostMapping("/{id}/logs")
    public ResponseEntity<BaseResponse<LogWarrantyResponse>> open(
            @Parameter(description = "ID của Warranty cần mở ticket", example = "b2e3a9d4-0f4f-4b1f-8d71-4d5ab7f3c9af")
            @PathVariable UUID id,
            @RequestBody WarrantyLogOpenRequest req
    ) {
        LogWarrantyResponse log = warrantyService.openTicket(id, req);
        return ResponseEntity.ok(BaseResponse.success("Ticket opened", log));
    }

    // ============================================================
    // 🔁 Cập nhật ticket bảo hành
    // ============================================================
    @Operation(
            summary = "Cập nhật trạng thái/chi tiết ticket",
            description = """
            • Đổi `status` và cập nhật chi tiết xử lý.  
            • Chuỗi trạng thái khuyến nghị:
              `OPEN → DIAGNOSING → (WAITING_PARTS | REPAIRING) → READY_FOR_PICKUP / SHIP_BACK → COMPLETED → CLOSED`  
            • Query param:
              - `status` (bắt buộc)  
            • Body (tùy chọn):
              - `diagnosis`, `resolution`, `shipBackTracking`  
              - `attachmentUrls` (ghi đè toàn bộ danh sách)  
              - `costLabor`, `costParts` (nếu `covered=false` hệ thống tự set `costTotal = labor + parts`)  
            • Kết quả: `LogWarrantyResponse` sau cập nhật.  
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Thông tin cập nhật (tùy chọn)",
                    required = false,
                    content = @Content(schema = @Schema(implementation = WarrantyLogUpdateRequest.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cập nhật ticket thành công",
                    content = @Content(schema = @Schema(implementation = LogWarrantyResponse.class))
            )
    })
    @PatchMapping("/logs/{logId}")
    public ResponseEntity<BaseResponse<LogWarrantyResponse>> update(
            @Parameter(description = "ID của LogWarranty", example = "a7f6d2e3-1b2c-4d5e-9f0a-1234567890ab")
            @PathVariable UUID logId,

            @Parameter(
                    description = "Trạng thái mới",
                    schema = @Schema(allowableValues = {
                            "OPEN","DIAGNOSING","WAITING_PARTS","REPAIRING",
                            "READY_FOR_PICKUP","SHIP_BACK","COMPLETED","CLOSED"
                    })
            )
            @RequestParam WarrantyLogStatus status,

            @RequestBody(required = false) WarrantyLogUpdateRequest req
    ) {
        LogWarrantyResponse updated = warrantyService.updateTicketStatus(logId, status, req);
        return ResponseEntity.ok(BaseResponse.success("Ticket updated", updated));
    }

    // ============================================================
    // ⭐ Gửi đánh giá sau bảo hành
    // ============================================================
    @Operation(
            summary = "Gửi review cho một lần bảo hành",
            description = """
            • Chỉ cho phép **đúng khách hàng sở hữu Warranty** review.  
            • Query param:
              - `customerId` — ID khách gửi đánh giá.  
            • Body:
              - `rating` (1..5), `comment` (tùy chọn).  
            • Kết quả: `WarrantyReviewResponse`.  
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nội dung đánh giá",
                    required = true,
                    content = @Content(schema = @Schema(implementation = WarrantyReviewRequest.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gửi đánh giá thành công",
                    content = @Content(schema = @Schema(implementation = WarrantyReviewResponse.class))
            )
    })
    @PostMapping("/logs/{logId}/review")
    public ResponseEntity<BaseResponse<WarrantyReviewResponse>> review(
            @Parameter(description = "ID của LogWarranty", example = "a7f6d2e3-1b2c-4d5e-9f0a-1234567890ab")
            @PathVariable UUID logId,

            @Parameter(description = "ID khách hàng gửi đánh giá", example = "7e442765-ed42-4a14-9181-b1a286bc8276")
            @RequestParam UUID customerId,

            @RequestBody WarrantyReviewRequest req
    ) {
        WarrantyReviewResponse review = warrantyService.review(logId, customerId, req);
        return ResponseEntity.ok(BaseResponse.success("Review submitted", review));
    }
}