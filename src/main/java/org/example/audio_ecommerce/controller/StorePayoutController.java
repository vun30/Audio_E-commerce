package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StorePayoutItemResponse;
import org.example.audio_ecommerce.dto.response.StorePayoutSummaryResponse;
import org.example.audio_ecommerce.entity.Enum.StorePayoutBucket;
import org.example.audio_ecommerce.service.StorePayoutProcessService;
import org.example.audio_ecommerce.service.StorePayoutQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Store Payout Overview", description = "API tổng quan & breakdown payout của shop")
@RestController
@RequestMapping("/api/stores/me/payout") // ✅ FIX: tách khỏi /wallet để tránh Ambiguous mapping
@RequiredArgsConstructor
public class StorePayoutController {

    private final StorePayoutQueryService storePayoutQueryService;
    private final StorePayoutProcessService storePayoutProcessService;

    @Operation(
            summary = "Tổng quan tiền payout của shop theo khoảng thời gian",
            description = """
                Trả về 3 nhóm:
                - Pending Balance: item đã delivered nhưng eligibleForPayout=false (đang bị hold)
                - Platform Fee Payable: eligibleForPayout=true nhưng isPayout=false (phí nền tảng sẽ thu)
                - Available Balance: eligibleForPayout=true và isPayout=true (shop có thể rút)

                ✅ StoreId tự lấy từ token JWT (không cần truyền storeId).
                ✅ Loại trừ item bị return.
                ✅ Nếu không truyền from/to, hệ thống tự chọn khoảng mặc định.
                
                               {
                                         "status": 200,                         // HTTP status logic (API thành công)
                                         "message": "✅ Lấy payout summary thành công",
                                         "data": {
                                   
                                           "pendingCount": 0,                   // 🔒 Số order item đã DELIVERED
                                                                                 // nhưng CHƯA đủ điều kiện payout
                                                                                 // (eligibleForPayout = false / null)
                                                                                 // → tiền còn đang bị HOLD
                                   
                                           "pendingGross": 0,                   // 🔒 Tổng tiền gốc (SUM finalLineTotal)
                                                                                 // của các item đang HOLD
                                                                                 // → tiền này shop CHƯA được dùng
                                   
                                           "eligibleNotPayoutCount": 0,          // ⏳ Số order item đã đủ điều kiện payout
                                                                                 // (eligibleForPayout = true)
                                                                                 // nhưng CHƯA payout (isPayout = false)
                                   
                                           "eligibleNotPayoutGross": 0,          // ⏳ Tổng tiền gốc (SUM finalLineTotal)
                                                                                 // của các item đã eligible nhưng
                                                                                 // hệ thống CHƯA giải ngân cho shop
                                   
                                           "platformFeePayable": 0,              // 🧾 Tổng PHÍ NỀN TẢNG PHẢI THU
                                                                                 // = SUM(finalLineTotal * platformFee%)
                                                                                 // của nhóm eligibleNotPayout
                                                                                 // → phí này CHƯA bị trừ
                                   
                                           "payoutDoneCount": 1,                 // ✅ Số order item đã PAYOUT xong
                                                                                 // (eligibleForPayout = true
                                                                                 //  AND isPayout = true)
                                   
                                           "availableGross": 32800,              // 💰 Tổng tiền gốc của các item
                                                                                 // đã payout cho shop
                                                                                 // = SUM(finalLineTotal)
                                   
                                           "platformFeePaid": 3280,              // 🏦 Tổng PHÍ NỀN TẢNG ĐÃ TRỪ
                                                                                 // = SUM(finalLineTotal * platformFee%)
                                                                                 // của các item đã payout
                                                                                 // (ví dụ 10% của 32,800)
                                   
                                           "availableNet": 29520                 // 💵 TIỀN SHOP THỰC NHẬN / CÓ THỂ RÚT
                                                                                 // = availableGross - platformFeePaid
                                                                                 // = 32,800 - 3,280 = 29,520
                                         }
                                         
                                         
                                                               2099-12-31T23:59:59     nhớ nập dữ liệu đúng thời gian
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy payout summary thành công",
                    content = @Content(schema = @Schema(implementation = StorePayoutSummaryResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / token không hợp lệ")
    })
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<StorePayoutSummaryResponse>> getSummary(
            @Parameter(description = "Từ ngày deliveredAt (ISO). Optional")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Đến ngày deliveredAt (ISO). Optional")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        StorePayoutSummaryResponse data = storePayoutQueryService.getSummary(from, to);
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy payout summary thành công", data));
    }

    @Operation(
            summary = "Breakdown order items theo từng bucket payout",
            description = """
                Trả về danh sách StoreOrderItem theo bucket:
                - PENDING
                - ELIGIBLE_NOT_PAYOUT
                - PAYOUT_DONE
                
                
                for example: 2026-12-17T12:47:19.848Z

                ✅ Hỗ trợ lọc theo khoảng thời gian deliveredAt (from/to), phân trang.
                ✅ StoreId tự lấy từ token JWT.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy payout breakdown items thành công",
                    content = @Content(schema = @Schema(implementation = PagedResult.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bucket không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / token không hợp lệ")
    })
    @GetMapping("/items")
    public ResponseEntity<BaseResponse<PagedResult<StorePayoutItemResponse>>> getItems(
            @Parameter(description = "Bucket: PENDING | ELIGIBLE_NOT_PAYOUT | PAYOUT_DONE", required = true)
            @RequestParam StorePayoutBucket bucket,

            @Parameter(description = "Từ ngày deliveredAt (ISO). Optional")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Đến ngày deliveredAt (ISO). Optional")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Trang (default=0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Size (default=20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<StorePayoutItemResponse> data =
                storePayoutQueryService.getBreakdownItems(bucket, from, to, page, size);

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy payout breakdown items thành công", data));
    }

    // POST vì nó tạo transaction + update DB

    @Operation(
            summary = "Rút tiền tự động các item đủ điều kiện payout",
            description = """
                Tự động đánh dấu tất cả các StoreOrderItem đủ điều kiện payout
                (eligibleForPayout = true AND isPayout = false)
                thành đã payout (isPayout = true),
                và tạo transaction ghi nhận việc rút tiền vào ví của shop.

                ✅ StoreId tự lấy từ token JWT.
                ✅ Hệ thống chỉ xử lý các item chưa payout.
                ✅ Trả về tổng số item đã payout và tổng số tiền net đã chuyển vào ví shop.
            """
    )
    @PostMapping("/auto-process")
    public ResponseEntity<BaseResponse> processMyEligiblePayoutItems() {
        return storePayoutProcessService.processMyEligiblePayoutItems();
    }
}
