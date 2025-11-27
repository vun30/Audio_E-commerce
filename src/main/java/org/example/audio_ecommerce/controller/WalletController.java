package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.WalletTxnRequest;
import org.example.audio_ecommerce.dto.response.WalletResponse;
import org.example.audio_ecommerce.dto.response.WalletTransactionResponse;
import org.example.audio_ecommerce.service.WalletService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

@Tag(name = "Wallet", description = "Các API thao tác ví điện tử của khách hàng")
@RestController
@RequestMapping("/api/customers/{customerId}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(
            summary = "Lấy lịch sử giao dịch ví",
            description = "Trả về danh sách giao dịch (mới nhất trước). Hỗ trợ phân trang qua tham số `page`, `size`."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy lịch sử thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WalletTransactionResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví của customer")
    })
    @GetMapping("/transactions")
    public Page<WalletTransactionResponse> list(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "Trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return walletService.listTransactions(customerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Operation(summary = "Nạp tiền vào ví", description = "Tăng số dư ví. Không yêu cầu orderId.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nạp tiền thành công",
                    content = @Content(schema = @Schema(implementation = WalletTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví"),
            @ApiResponse(responseCode = "400", description = "amount không hợp lệ")
    })
    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletTransactionResponse deposit(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody(
                    required = true,
                    description = "Thông tin nạp tiền",
                    content = @Content(schema = @Schema(implementation = WalletTxnRequest.class),
                            examples = @ExampleObject(name = "Topup", value = """
                        { "amount": 100000, "description": "Topup MoMo" }
                    """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody WalletTxnRequest req) {
        return walletService.deposit(customerId, req);
    }

    @Operation(summary = "Rút tiền khỏi ví", description = "Giảm số dư ví. Yêu cầu đủ số dư.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rút tiền thành công",
                    content = @Content(schema = @Schema(implementation = WalletTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví"),
            @ApiResponse(responseCode = "400", description = "amount không hợp lệ / không đủ số dư")
    })
    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletTransactionResponse withdraw(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody(
                    required = true,
                    description = "Thông tin rút tiền",
                    content = @Content(schema = @Schema(implementation = WalletTxnRequest.class),
                            examples = @ExampleObject(name = "Withdraw", value = """
                        { "amount": 50000, "description": "Rút về ngân hàng" }
                    """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody WalletTxnRequest req) {
        return walletService.withdraw(customerId, req);
    }

    @Operation(summary = "Thanh toán đơn hàng", description = "Trừ tiền ví để thanh toán đơn hàng. Bắt buộc có `orderId`. Có idempotency theo (customerId, orderId).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Thanh toán thành công",
                    content = @Content(schema = @Schema(implementation = WalletTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví"),
            @ApiResponse(responseCode = "400", description = "Thiếu orderId / amount không hợp lệ / không đủ số dư")
    })
    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletTransactionResponse payment(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody(
                    required = true,
                    description = "Thông tin thanh toán",
                    content = @Content(schema = @Schema(implementation = WalletTxnRequest.class),
                            examples = @ExampleObject(name = "Payment", value = """
                        { "amount": 120000, "orderId": "e1c0a3c1-3b3f-4d3b-9b5f-9a2a1f2f3e4d", "description": "Pay order #123" }
                    """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody WalletTxnRequest req) {
        return walletService.payment(customerId, req);
    }

    @Operation(summary = "Hoàn tiền đơn hàng", description = "Cộng tiền về ví. Bắt buộc có `orderId`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hoàn tiền thành công",
                    content = @Content(schema = @Schema(implementation = WalletTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví"),
            @ApiResponse(responseCode = "400", description = "Thiếu orderId / amount không hợp lệ")
    })
    @PostMapping("/refund")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletTransactionResponse refund(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody(
                    required = true,
                    description = "Thông tin hoàn tiền",
                    content = @Content(schema = @Schema(implementation = WalletTxnRequest.class),
                            examples = @ExampleObject(name = "Refund", value = """
                        { "amount": 120000, "orderId": "e1c0a3c1-3b3f-4d3b-9b5f-9a2a1f2f3e4d", "description": "Refund order #123" }
                    """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody WalletTxnRequest req) {
        return walletService.refund(customerId, req);
    }

    @Operation(
            summary = "Lấy thông tin ví của khách hàng",
            description = "Trả về thông tin ví: số dư hiện tại, trạng thái, currency, thời gian giao dịch gần nhất."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy ví thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ví của customer")
    })
    @GetMapping
    public WalletResponse getWallet(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId
    ) {
        return walletService.getByCustomer(customerId);
    }

}

