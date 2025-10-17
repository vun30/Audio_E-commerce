package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ShopVoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Shop Voucher", description = "API quản lý mã giảm giá của cửa hàng (shop vouchers)")
@RestController
@RequestMapping("/api/shop-vouchers")
@RequiredArgsConstructor
public class ShopVoucherController {

    private final ShopVoucherService service;

    // ============================================================
    // ➕ CREATE
    // ============================================================
    @Operation(
            summary = "Tạo mới voucher cho nhiều sản phẩm",
            description = """
                    Cho phép cửa hàng tạo voucher và áp dụng cho nhiều sản phẩm.
                    <br><br>⚙️ **Logic tự động:**
                    - Nếu `discountPercent` khác null → áp dụng giảm theo %.
                    - Nếu `discountAmount` khác null → áp dụng giảm theo số tiền cố định.
                    - Nếu cả 2 đều null → giữ nguyên giá.
                    <br><br>✅ **Lưu ý:** Chỉ có thể áp voucher cho sản phẩm thuộc chính cửa hàng đó.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Thông tin tạo voucher mới",
                    content = @Content(
                            schema = @Schema(implementation = ShopVoucherRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SALE10K",
                                      "title": "Giảm 10K toàn shop",
                                      "description": "Áp dụng cho đơn hàng từ 100K trở lên",
                                      "type": "FIXED",
                                      "discountValue": 10000,
                                      "discountPercent": null,
                                      "maxDiscountValue": null,
                                      "minOrderValue": 100000,
                                      "totalVoucherIssued": 100,
                                      "totalUsageLimit": 100,
                                      "usagePerUser": 2,
                                      "startTime": "2025-10-20T00:00:00",
                                      "endTime": "2025-11-20T23:59:59",
                                      "products": [
                                        {
                                          "productId": "b6dbb60e-bfe5-4e5f-ae7f-bcfb9a1b529a",
                                          "discountPercent": 10,
                                          "discountAmount": null,
                                          "promotionStockLimit": 50,
                                          "purchaseLimitPerCustomer": 2
                                        },
                                        {
                                          "productId": "a2c44cda-1f44-4d9a-84e9-6f2b4f5e8a7a",
                                          "discountPercent": null,
                                          "discountAmount": 20000,
                                          "promotionStockLimit": 30,
                                          "purchaseLimitPerCustomer": 1
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Voucher created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid data or logic error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createVoucher(@RequestBody ShopVoucherRequest req) {
        return service.createVoucher(req);
    }

    // ============================================================
    // 📦 GET ALL
    // ============================================================
    @Operation(
            summary = "Lấy tất cả voucher của cửa hàng hiện tại",
            description = """
                    Trả về danh sách tất cả voucher thuộc về cửa hàng đang đăng nhập.
                    Bao gồm các voucher đang hoạt động, đã hết hạn hoặc bị vô hiệu hóa.
                    """
    )
    @GetMapping
    public ResponseEntity<BaseResponse> getAllVouchers() {
        return service.getAllVouchers();
    }

    // ============================================================
    // 🔍 GET BY ID
    // ============================================================
    @Operation(
            summary = "Xem chi tiết voucher theo ID",
            description = """
                    Lấy chi tiết voucher bao gồm thông tin cấu hình và danh sách sản phẩm áp dụng.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Voucher detail retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Voucher not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getVoucherById(@PathVariable UUID id) {
        return service.getVoucherById(id);
    }

    // ============================================================
    // 🚫 DISABLE / ENABLE
    // ============================================================
    @Operation(
            summary = "Bật / Tắt trạng thái voucher",
            description = """
                    Cho phép admin hoặc chủ shop chuyển đổi trạng thái voucher.
                    <br><br>
                    - Nếu voucher đang **ACTIVE** → đổi sang **DISABLED**.  
                    - Nếu voucher đang **DISABLED** → đổi lại **ACTIVE**.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Voucher status toggled successfully"),
                    @ApiResponse(responseCode = "404", description = "Voucher not found")
            }
    )
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BaseResponse> toggleVoucher(@PathVariable UUID id) {
        return service.disableVoucher(id);
    }
}
