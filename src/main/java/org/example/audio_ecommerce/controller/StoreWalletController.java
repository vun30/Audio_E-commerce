package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.DepositTransferRequest;
import org.example.audio_ecommerce.dto.request.WithdrawDepositToDefaultRequest;
import org.example.audio_ecommerce.dto.request.WithdrawRequest;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.service.PlatformWalletService;
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
    private final PlatformWalletService platformWalletService;

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
    public ResponseEntity<BaseResponse> filterTransactions(
            @RequestParam(required = false) UUID walletId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID payoutRequestId,

            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,          // ✅ lọc theo từng loại tran
            @RequestParam(required = false) WalletBucket bucket,
            @RequestParam(required = false) TxDirection direction,
            @RequestParam(required = false) PaymentChannel channel,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PlatformTransactionResponse> result =
                platformWalletService.filterFlatWalletTransactions(
                        storeId, customerId, orderId, payoutRequestId,
                        status, type, bucket, direction, channel,
                        from, to, pageable
                );

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy danh sách giao dịch platform thành công", result));
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
                     case DEPOSIT -> "Tiền bán hàng (payout vào ví)";
                            case PENDING_HOLD -> "Giữ tiền tạm thời (pending hold)";
                            case RELEASE_PENDING -> "Giải phóng tiền giữ (pending → default)";
                            case WITHDRAW -> "Rút tiền về ngân hàng";
                            case REFUND -> "Hoàn tiền cho khách";
                            case ADJUSTMENT -> "Điều chỉnh thủ công (admin)";
                            case REFUND_RETURN -> "Hoàn tiền do trả hàng";
                            case REFUND_FORCE -> "Hoàn tiền cưỡng chế";
                            case TOPUP -> "Nạp tiền vào ví";
                            case DEBT_PAYMENT -> "Thanh toán nợ";
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
            summary = "Tổng quan ví payout theo từng order item // BỎ KO DÙNG",
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

    @Operation(
            summary = "Thanh toán nợ cửa hàng từ ví khả dụng (defaultBalance)",
            description = """
                API cho phép cửa hàng đang đăng nhập thanh toán **các khoản nợ ĐÃ CHỐT (real debt)** 
                bằng tiền trong ví `defaultBalance`.

                🔹 Phạm vi thanh toán:
                - Chỉ thanh toán các khoản nợ đã kết thúc trạng thái (DELIVERED, RETURNED).
                - Bao gồm:
                  • Chênh lệch phí ship (SHIP_DIFF)
                  • Phí quay đầu / không nhận hàng đã chốt (RTO_FEE)
                  • Phí hoàn/return mà SHOP chịu (RETURN_SHIPPING_FEE)

                🔹 KHÔNG thanh toán:
                - Các khoản nợ ảo / nợ tạm (đơn chưa end status).
                - Các khoản phí chưa được xác nhận bởi hệ thống.

                🔹 Quy trình xử lý:
                1️⃣ Kiểm tra cửa hàng từ token đăng nhập (không cần truyền storeId).
                2️⃣ Tính tổng nợ real chưa thanh toán.
                3️⃣ Kiểm tra đủ tiền trong `defaultBalance`.
                4️⃣ Trừ tiền từ ví.
                5️⃣ Lưu lịch sử giao dịch (DEBT_PAYMENT).
                6️⃣ Đánh dấu các khoản nợ là đã thanh toán.
                7️⃣ Tính lại `debtBalance`.
                8️⃣ Tự động kiểm tra mở khóa cửa hàng (nếu đang bị khóa do nợ).

                🔹 Lưu ý:
                - Nếu số dư không đủ → giao dịch bị từ chối và KHÔNG ghi nhận transaction.
                - Việc mở khóa phụ thuộc điều kiện:
                  debt < (deposit + legalPoint × 100.000)
                  và deposit ≥ 10% debt
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Thanh toán nợ thành công",
                    content = @Content(schema = @Schema(implementation = PayDebtResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Không có khoản nợ nào cần thanh toán"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Số dư ví không đủ để thanh toán nợ"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Chưa đăng nhập hoặc token không hợp lệ"
            )
    })
    @PostMapping("/debt/pay")
    public ResponseEntity<BaseResponse> payMyDebt() {
        return storeWalletService.payMyDebtFromDefaultBalance();
    }

    @Operation(
            summary = "Rút tiền từ ví khả dụng (defaultBalance)",
            description = """
                API cho phép cửa hàng đang đăng nhập rút tiền từ ví `defaultBalance`.

                Quy trình:
                1) Lấy store từ token (không cần truyền storeId)
                2) Kiểm tra số dư defaultBalance
                3) Trừ tiền và lưu StoreWalletTransaction (WITHDRAW)
                4) Lưu PlatformTransaction để truy soát

                Lưu ý:
                - Nếu số dư không đủ: từ chối và KHÔNG ghi nhận transaction.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Rút tiền thành công",
                    content = @Content(schema = @Schema(implementation = WithdrawResult.class))
            ),
            @ApiResponse(responseCode = "409", description = "Số dư không đủ để rút"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / token không hợp lệ")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<BaseResponse> withdrawFromDefault(@RequestBody WithdrawRequest req) {
        return storeWalletService.withdrawFromDefaultBalance(req);
    }


    @Operation(
            summary = "Chuyển tiền từ ví default sang ví cọc (deposit)",
            description = """
                API cho phép cửa hàng đang đăng nhập chuyển một khoản tiền tùy chọn từ:
                - defaultBalance -> depositBalance

                Quy trình:
                1) Lấy store từ token (không cần truyền storeId)
                2) Kiểm tra defaultBalance đủ tiền
                3) Trừ defaultBalance, cộng depositBalance
                4) Lưu StoreWalletTransaction (TRANSFER_TO_DEPOSIT)
                
                Lưu ý:
                - Nếu không đủ tiền => từ chối và không ghi transaction.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Chuyển thành công",
                    content = @Content(schema = @Schema(implementation = DepositTransferResult.class))
            ),
            @ApiResponse(responseCode = "409", description = "defaultBalance không đủ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / token không hợp lệ")
    })
    @PostMapping("/deposit/transfer-in")
    public ResponseEntity<BaseResponse> transferDefaultToDeposit(@RequestBody DepositTransferRequest req) {
        return storeWalletService.transferDefaultToDeposit(req);
    }

    @Operation(
            summary = "Rút tiền từ ví cọc (depositBalance) về ví defaultBalance",
            description = """
                API cho phép cửa hàng đang đăng nhập chuyển một khoản tiền từ ví cọc sang ví default.

                ✅ Điều kiện bắt buộc:
                - amount > 0
                - depositBalance đủ để rút
                - Sau khi rút phải đảm bảo: 
                  creditAfter = depositAfter + legalPoint * 100000  >= debtBalance

                Nếu không thỏa điều kiện => trả lỗi và KHÔNG ghi nhận transaction.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rút thành công"),
            @ApiResponse(responseCode = "409", description = "Không đủ điều kiện rút (deposit không đủ hoặc creditAfter < debt)"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / token không hợp lệ")
    })
    @PostMapping("/deposit/withdraw-to-default")
    public ResponseEntity<BaseResponse> withdrawDepositToDefault(@Valid @RequestBody WithdrawDepositToDefaultRequest req) {
        return storeWalletService.withdrawDepositToDefault(req);
    }

    @Operation(
            summary = "Tổng quan ví store đang đăng nhập",
            description = """
                Trả về các số dư chính của ví shop:
                - defaultBalance: số dư nạp / số dư khả dụng trong hệ thống theo bạn định nghĩa
                - depositBalance: tiền ký quỹ
                - debtBalance: số nợ hiện tại
                
                ✅ StoreId tự lấy từ token JWT.
            """
    )
    @GetMapping("/overview")
    public ResponseEntity<BaseResponse<StoreWalletOverviewResponse>> getOverview() {
        StoreWalletOverviewResponse data = storeWalletService.getMyWalletOverview();
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy tổng quan ví thành công", data));
    }


}
