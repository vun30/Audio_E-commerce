package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.service.PlatformWalletService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.FlatGhnOverviewResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "Platform Wallet",
        description = """
                Các API quản lý ví trung gian của nền tảng.
                Bao gồm: ví của hệ thống (Platform), ví cửa hàng (Shop), và ví khách hàng (Customer).  
                Hỗ trợ xem danh sách, xem chi tiết ví, và lọc giao dịch.
                """
)
@RestController
@RequestMapping("/api/platform-wallets")
@RequiredArgsConstructor
public class PlatformWalletController {

    private final PlatformWalletService walletService;
    private final PlatformWalletService platformWalletService;

    // ==============================
    // 🪙 LẤY DANH SÁCH TẤT CẢ VÍ
    // ==============================
    @Operation(
            summary = "Lấy danh sách tất cả ví",
            description = """
                    - API này trả về danh sách toàn bộ ví trong hệ thống (bao gồm Platform, Shop, Customer).  
                    - Mỗi ví chứa thông tin cơ bản như số dư, tiền pending, tiền done, tổng refund, v.v.  
                    - Dùng cho dashboard admin hoặc thống kê hệ thống.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách ví thành công",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlatformWalletResponse.class)))),
    })
    @GetMapping
    public ResponseEntity<List<PlatformWalletResponse>> getAllWallets() {
        return ResponseEntity.ok(walletService.getAllWallets());
    }

    // ==============================
    // 👤 LẤY VÍ THEO OWNER ID
    // ==============================
    @Operation(
            summary = "Lấy ví của chủ sở hữu (Shop/Customer)",
            description = """
                    - Dùng để lấy chi tiết ví dựa theo `ownerId` (ID của shop hoặc customer).  
                    - API này cũng trả về danh sách **giao dịch (transactions)** của ví đó.  
                    - Hữu ích cho màn hình "Chi tiết ví" của từng cửa hàng hoặc người dùng.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy ví thành công",
                    content = @Content(schema = @Schema(implementation = PlatformWalletResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví cho ownerId này")
    })
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<PlatformWalletResponse> getWalletByOwner(
            @Parameter(description = "ID của chủ sở hữu (Shop hoặc Customer)", required = true, example = "d7f1c3c8-0b33-49d4-bad4-9e0bb2b8b9a3")
            @PathVariable UUID ownerId) {
        return ResponseEntity.ok(walletService.getWalletByOwner(ownerId));
    }

    // ==============================
    // 🔍 LỌC GIAO DỊCH
    // ==============================
    @Operation(
            summary = "Lọc danh sách giao dịch (shop hoặc customer)",
            description = """
                    - API cho phép lọc danh sách **transaction** theo nhiều tiêu chí:  
                      • `storeId`: lọc giao dịch theo cửa hàng.  
                      • `customerId`: lọc giao dịch theo khách hàng.  
                      • `status`: trạng thái giao dịch (`PENDING`, `DONE`, `FAILED`).  
                      • `type`: loại giao dịch (`HOLD`, `RELEASE`, `REFUND`, `WITHDRAW`, `DEPOSIT`, ...).  
                      • `from`, `to`: khoảng thời gian bắt đầu và kết thúc (ISO date).  
                    - Các tham số đều là **tuỳ chọn**, có thể kết hợp nhiều điều kiện cùng lúc.  
                    - Kết quả trả về là danh sách các giao dịch đã lọc.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lọc giao dịch thành công",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlatformTransactionResponse.class)))),
    })
    @GetMapping("/transactions/filter")
    public ResponseEntity<List<PlatformTransactionResponse>> filterTransactions(
            @Parameter(description = "ID cửa hàng cần lọc (UUID)", example = "d7f1c3c8-0b33-49d4-bad4-9e0bb2b8b9a3")
            @RequestParam(required = false) UUID storeId,

            @Parameter(description = "ID khách hàng cần lọc (UUID)", example = "a5e1f3b8-2d44-4ef1-bcd4-98c12aee99ff")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Trạng thái giao dịch (PENDING, DONE, FAILED)", example = "DONE")
            @RequestParam(required = false) TransactionStatus status,

            @Parameter(description = "Loại giao dịch (HOLD, RELEASE, REFUND, WITHDRAW, ...)", example = "REFUND")
            @RequestParam(required = false) TransactionType type,

            @Parameter(description = "Ngày bắt đầu lọc (ISO format)", example = "2025-10-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Ngày kết thúc lọc (ISO format)", example = "2025-10-12T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        return ResponseEntity.ok(walletService.filterTransactions(storeId, customerId, status, type, from, to));
    }

    // ==============================
// 🏦 LẤY VÍ CỦA PLATFORM
// ==============================
    @Operation(
            summary = "Lấy ví của Platform (hệ thống)",
            description = """
                    - API trả về ví duy nhất của nền tảng.  
                    - Ví Platform giữ tiền khách thanh toán online (HOLD),  
                      sau đó phân phối cho shop khi đủ điều kiện.  
                    - Dữ liệu bao gồm số dư tổng, pending, done và lịch sử giao dịch.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy ví platform thành công",
                    content = @Content(schema = @Schema(implementation = PlatformWalletResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví platform")
    })
    @GetMapping("/platform")
    public ResponseEntity<PlatformWalletResponse> getPlatformWallet() {
        return ResponseEntity.ok(walletService.getPlatformWallet());
    }

    // ==============================
    // 📊 LẤY TỔNG QUAN VÍ PLATFORM
    // ==============================
    @Operation(
            summary = "Lấy tổng quan ví Platform (Overview)",
            description = """
                    - API trả về thông tin tổng quan ví Platform.
                    - Bao gồm: tổng tiền nạp, tiền pending, tiền done, phí commission, etc.
                    - Hữu ích cho dashboard admin theo dõi trạng thái ví nền tảng.
                    - Hiển thị số lượng order pending và done.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy overview thành công",
                    content = @Content(schema = @Schema(implementation = org.example.audio_ecommerce.dto.response.PlatformWalletOverviewResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/platform/overview")
    public ResponseEntity<org.example.audio_ecommerce.dto.response.BaseResponse<org.example.audio_ecommerce.dto.response.PlatformWalletOverviewResponse>> getPlatformWalletOverview() {
        var overview = walletService.getPlatformWalletOverview();
        return ResponseEntity.ok(
                org.example.audio_ecommerce.dto.response.BaseResponse.success(
                        "Tổng quan ví platform",
                        overview
                )
        );
    }

    // ==============================
// 📜 GIAO DỊCH VÍ PLATFORM (FLAT WALLET)
// Chỉ filter theo type/status/khoảng ngày + paging
// ==============================
    @Operation(
            summary = "Lấy giao dịch ví Platform (flat wallet) - phân trang",
            description = """
                    Trả về danh sách giao dịch thuộc ví Platform (ví tổng duy nhất).
                    
                    ✅ Bộ lọc hỗ trợ:
                    - type: loại giao dịch (HOLD, RELEASE, WITHDRAW, PAYOUT_STORE, ...). null = tất cả
                    - status: trạng thái giao dịch. null = tất cả
                    - from/to: lọc theo createdAt. null = không giới hạn
                    
                    ✅ Có phân trang.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy giao dịch thành công",
                    content = @Content(schema = @Schema(implementation = org.springframework.data.domain.Page.class)))
    })
    @GetMapping("/platform/transactions")
    public ResponseEntity<org.example.audio_ecommerce.dto.response.BaseResponse<Page<PlatformTransactionResponse>>> getPlatformTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        Page<PlatformTransactionResponse> result =
                walletService.getFlatWalletTransactions(type, status, from, to, pageable);

        return ResponseEntity.ok(
                org.example.audio_ecommerce.dto.response.BaseResponse.success("Danh sách giao dịch ví platform", result)
        );

    }



    @Operation(
            summary = "Tổng quan GHN của ví tổng (Flat) - nợ GHN, ship khách trả, nợ shop",
            description = """
                API trả về các số liệu tài chính liên quan đến GHN và nghĩa vụ thanh toán của ví tổng (Flat).

                ✅ 1) flatDebtShipToGHN (Flat nợ GHN - theo phí ship thực tế):
                - Chỉ tính các đơn thỏa điều kiện phát sinh nợ GHN:
                  • shippingFeeReal > 0
                  • status KHÔNG thuộc: UNPAID, CONFIRMED, AWAITING_SHIPMENT, EXCEPTION, CANCELLED
                  • và thuộc 1 trong 2 case:
                    (A) deliveredAt != null  → nợ = shippingFeeReal
                    (B) deliveredAt == null & status = RETURNING → nợ = shippingFeeReal * 1.5
                - Ý nghĩa: Tổng tiền ship GHN mà Flat phải thanh toán cho GHN.

                ✅ 2) customerShipPaid (Khách đã trả ship):
                - Chỉ cộng shippingFee của các đơn đã deliveredAt != null.
                - Ý nghĩa: Tổng tiền ship khách đã thanh toán (phần ship thu từ khách) cho các đơn giao thành công.

                ✅ 3) storeDebtOutstandingToFlat (Shop còn nợ Flat - snapshot ví shop):
                - Tính bằng SUM(store_wallets.debt_balance) của toàn hệ thống.
                - Ý nghĩa: Tổng số nợ hiện tại các shop còn đang nợ Flat (không phụ thuộc from/to nếu bạn để from/to null).

                ✅ 4) storeDebtPaidToFlat (Shop đã trả Flat - dựa trên StoreOrder):
                - Tính từ các đơn thỏa điều kiện nợ GHN ở mục (1) và có paidByShop = true.
                - Giá trị mỗi đơn được tính theo cùng rule nợ GHN:
                  delivered → shippingFeeReal
                  returning-not-delivered → shippingFeeReal * 1.5
                - Ý nghĩa: Tổng số tiền shop đã thanh toán hoàn lại cho Flat (để Flat trả GHN) theo đơn hàng.

                ✅ 5) storeDebtTotalToFlat:
                - storeDebtTotalToFlat = storeDebtPaidToFlat + storeDebtOutstandingToFlat
                - Ý nghĩa: Tổng nghĩa vụ nợ shop đối với Flat (đã trả + còn nợ).

                🔎 Bộ lọc thời gian:
                - from/to (optional): nếu truyền thì lọc theo createdAt của StoreOrder trong khoảng thời gian.
                - Nếu không truyền: thống kê toàn bộ dữ liệu.
                
                Gợi ý UI: dùng cho dashboard admin theo dõi nợ GHN & tình trạng shop trả nợ.
                """
    )
    @GetMapping("/ghn/overview")
    public ResponseEntity<BaseResponse<FlatGhnOverviewResponse>> getFlatGhnOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        FlatGhnOverviewResponse data = walletService.getFlatGhnOverview(from, to);

        return ResponseEntity.ok(
                BaseResponse.success("Lấy tổng quan GHN (Flat) thành công", data)
        );
    }
}
