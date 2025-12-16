package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.DebtComponentType;
import org.example.audio_ecommerce.entity.Enum.StoreWalletBucket;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.service.StoreWalletQueryService;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Store Wallet", description = "API quản lý ví cửa hàng (Store Wallet)")
@RestController
@RequestMapping("/api/stores/me/wallet")
@RequiredArgsConstructor
public class StoreWalletController {

    private final StoreWalletService storeWalletService;
    private final SecurityUtils securityUtils;

    // ✅ NEW: service chuyên dùng cho phần thống kê theo từng order item
    private final StoreWalletQueryService storeWalletQueryService;

    // =============================================================
    // 🏦 1️⃣ Lấy thông tin ví cửa hàng hiện tại
    // =============================================================
    @Operation(
            summary = "Lấy thông tin ví của cửa hàng đang đăng nhập",
            description = """
                    Trả về thông tin ví của cửa hàng (gồm số dư khả dụng, pending, deposit, tổng doanh thu, ...).
                    Hệ thống tự động lấy email từ token JWT của chủ cửa hàng.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin ví thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví cửa hàng")
    })
    @GetMapping
    public ResponseEntity<BaseResponse> getMyWallet() {
        return storeWalletService.getMyWallet();
    }

    // =============================================================
    // 📜 2️⃣ Lấy danh sách giao dịch ví (cơ bản, lọc theo type)
    // =============================================================
    @Operation(
            summary = "Lấy danh sách giao dịch ví (phân trang + lọc theo loại)",
            description = """
                    Trả về danh sách giao dịch của ví cửa hàng đang đăng nhập.
                    Hỗ trợ phân trang và lọc theo loại giao dịch (`DEPOSIT`, `WITHDRAW`, `REFUND`, ...).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách giao dịch thành công")
    })
    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse> getMyWalletTransactions(
            @Parameter(description = "Trang hiện tại (mặc định = 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng giao dịch mỗi trang (mặc định = 10)")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Loại giao dịch (tùy chọn)")
            @RequestParam(required = false) String type
    ) {
        return storeWalletService.getMyWalletTransactions(page, size, type);
    }

    // =============================================================
    // 🔍 3️⃣ Lọc giao dịch ví (theo thời gian, loại, ID giao dịch, storeId)
    // =============================================================
    @Operation(
            summary = "Lọc giao dịch ví theo thời gian, loại và ID giao dịch",
            description = """
                    Cho phép admin hoặc cửa hàng lọc danh sách giao dịch theo:
                    - `walletId` (tùy chọn): nếu không truyền → hệ thống tự động lấy ví của store đang login.
                    - `from` và `to`: khoảng thời gian (ISO format)
                    - `type`: loại giao dịch (`DEPOSIT`, `WITHDRAW`, `REFUND`, ...)
                    - `transactionId`: mã giao dịch cụ thể
                    - `sort`: định dạng "thuộc_tính:hướng" (VD: createdAt:desc)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lọc giao dịch thành công"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    @GetMapping("/filter")
    public ResponseEntity<BaseResponse> filterTransactions(
            @Parameter(description = "ID ví cửa hàng (tùy chọn — nếu không truyền, lấy của cửa hàng đang đăng nhập)")
            @RequestParam(required = false)
            UUID walletId,

            @Parameter(description = "Từ thời điểm (ISO format, VD: 2025-10-13T00:00:00)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Đến thời điểm (ISO format, VD: 2025-10-13T23:59:59)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Loại giao dịch (DEPOSIT, WITHDRAW, REFUND, ...)")
            @RequestParam(required = false)
            StoreWalletTransactionType type,

            @Parameter(description = "Mã giao dịch cụ thể (UUID)")
            @RequestParam(required = false)
            UUID transactionId,

            @Parameter(description = "Trang hiện tại (mặc định = 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng mỗi trang (mặc định = 10)")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sắp xếp, định dạng: 'thuộc_tính:hướng' (VD: createdAt:desc)")
            @RequestParam(defaultValue = "createdAt:desc") String sort
    ) {
        // ✅ Nếu không truyền walletId → tự động lấy ví của account đang đăng nhập
        if (walletId == null) {
            walletId = storeWalletService.resolveWalletIdForCurrentUser();
        }

        // ✅ Phân tích sort theo định dạng "property:direction"
        String[] parts = sort.split(":");
        String property = parts[0];
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1])
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));

        Page<StoreWalletTransactionResponse> transactions = storeWalletService.filterTransactions(
                walletId, from, to, type, transactionId, pageable
        );

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lọc giao dịch thành công", transactions)
        );
    }

    // =============================================================
    // 💰 4️⃣ TỔNG QUAN PAYOUT THEO ITEM (ước tính / pending / done / lãi ròng)
    // =============================================================
    @Operation(
            summary = "Tổng quan ví payout theo từng order item",
            description = """
                    Trả về 4 con số cho cửa hàng:
                    - estimatedGross: doanh thu ước tính (item chưa payout)
                    - pendingGross: doanh thu đang bị hold (chưa đủ điều kiện payout)
                    - doneGross: doanh thu đã payout (gross trước phí nền tảng + ship chênh lệch)
                    - netProfit: lãi ròng sau khi trừ phí nền tảng, ship chênh lệch, giá vốn.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy tổng quan payout thành công")
    })
    @GetMapping("/payout/summary")
    public ResponseEntity<BaseResponse<StoreWalletSummaryFinalResponse>> getPayoutSummary() {

        // ✅ Lấy storeId của cửa hàng đang đăng nhập
        UUID storeId = securityUtils.getCurrentStoreId();


        StoreWalletSummaryFinalResponse summary =
                storeWalletQueryService.getSummary(storeId);

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy tổng quan payout thành công", summary)
        );
    }

    // =============================================================
    // 📦 5️⃣ DANH SÁCH ORDER ITEM THEO BUCKET (estimated / pending / done)
    // =============================================================
    @Operation(
            summary = "Danh sách order item theo từng nhóm ví (estimated, pending, done)",
            description = """
                    Cho phép cửa hàng xem chi tiết các order item tạo nên từng con số ví:
                    - bucket=ESTIMATED: tất cả item chưa payout (doanh thu ước tính).
                    - bucket=PENDING: item chưa payout và chưa eligible_for_payout (đang hold).
                    - bucket=DONE: item đã payout, bao gồm cả lãi ròng từng item.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách item thành công")
    })
    @GetMapping("/payout/items")
    public ResponseEntity<BaseResponse<PagedResult<StoreWalletItemResponse>>> getPayoutItems(
            @Parameter(description = "Nhóm ví: ESTIMATED / PENDING / DONE", required = true)
            @RequestParam StoreWalletBucket bucket,

            @Parameter(description = "Trang hiện tại (mặc định = 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng mỗi trang (mặc định = 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        // ✅ Lấy storeId hiện tại
        UUID storeId = securityUtils.getCurrentStoreId();

        PagedResult<StoreWalletItemResponse> result =
                storeWalletQueryService.getItemsByBucket(storeId, bucket, page, size);

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy danh sách item theo bucket thành công", result)
        );
    }
}
