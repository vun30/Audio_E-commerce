package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PlatformTransactionResponse;
import org.example.audio_ecommerce.dto.response.PlatformWalletResponse;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.service.PlatformWalletService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
