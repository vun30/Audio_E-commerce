package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.request.ShopWideVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.service.ShopVoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Shop Voucher", description = "API quản lý mã giảm giá của cửa hàng (Shop Vouchers)")
@RestController
@RequestMapping("/api/shop-vouchers")
@RequiredArgsConstructor
public class ShopVoucherController {

    private final ShopVoucherService service;

    // ============================================================
    // ➕ CREATE VOUCHER
    // ============================================================
    @Operation(
            summary = "Tạo mới voucher cho nhiều sản phẩm",
            description = """
                    Cho phép cửa hàng tạo voucher và liên kết với nhiều sản phẩm.
                    <br><br>⚙️ **Logic hoạt động:**
                    - Voucher chỉ lưu điều kiện (giảm theo % hoặc số tiền cố định).
                    - Sản phẩm chỉ được liên kết với voucher, **không lưu giá giảm** trong DB.
                    - FE hoặc BE sẽ gọi API `calculate` để tính giá sau giảm tại runtime.
                    <br><br>✅ **Lưu ý:**  
                    - Chỉ có thể áp voucher cho sản phẩm thuộc chính cửa hàng.  
                    - Không ảnh hưởng đến giá gốc của sản phẩm (`Product.price`).
                     FIXED,      // Giảm số tiền cố định
                        PERCENT,    // Giảm phần trăm
                        SHIPPING    // Miễn phí vận chuyển
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
                                          "promotionStockLimit": 50,
                                          "purchaseLimitPerCustomer": 2
                                        },
                                        {
                                          "productId": "a2c44cda-1f44-4d9a-84e9-6f2b4f5e8a7a",
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
    // ➕ TẠO VOUCHER TOÀN SHOP (KHÔNG GIỚI HẠN, KHÔNG LIÊN KẾT SẢN PHẨM)
    // ============================================================
    /**
     * Tạo voucher áp dụng cho toàn bộ cửa hàng hoặc sản phẩm cụ thể.
     * FE cần truyền trường scopeType để chọn loại voucher:
     * - PRODUCT_VOUCHER: Áp dụng cho sản phẩm cụ thể (cần truyền products)
     * - ALL_SHOP_VOUCHER: Áp dụng toàn shop (không cần products)
     *
     * Ví dụ request tạo voucher toàn shop:
     * {
     *   "code": "SALEALL",
     *   "title": "Giảm 10% toàn shop",
     *   "description": "Áp dụng cho mọi đơn hàng",
     *   "type": "PERCENT",
     *   "discountPercent": 10,
     *   "minOrderValue": 100000,
     *   "startTime": "2025-12-01T00:00:00",
     *   "endTime": "2025-12-31T23:59:59",
     *   "scopeType": "ALL_SHOP_VOUCHER"
     * }
     *
     * Ví dụ request tạo voucher cho sản phẩm:
     * {
     *   "code": "SALEPROD",
     *   "title": "Giảm 10% cho sản phẩm",
     *   "type": "PERCENT",
     *   "discountPercent": 10,
     *   "products": [ ... ],
     *   "scopeType": "PRODUCT_VOUCHER"
     * }
     *
     * @param req Thông tin voucher toàn shop
     * @return ResponseEntity<BaseResponse>
     */
    @Operation(
        summary = "Tạo voucher toàn shop (không giới hạn, không liên kết sản phẩm)",
        description = "Tạo voucher áp dụng cho toàn bộ cửa hàng, không giới hạn số lượng, không liên kết sản phẩm.\n" +
                "FE chỉ cần truyền các trường cơ bản, không cần products, totalVoucherIssued, usagePerUser.\n" +
                "\nVí dụ request:\n" +
                "{\n" +
                "  \"code\": \"SALEALL\",\n" +
                "  \"title\": \"Giảm 10% toàn shop\",\n" +
                "  \"description\": \"Áp dụng cho mọi đơn hàng\",\n" +
                "  \"type\": \"PERCENT\",\n" +
                "  \"discountPercent\": 10,\n" +
                "  \"minOrderValue\": 100000,\n" +
                "  \"startTime\": \"2025-12-01T00:00:00\",\n" +
                "  \"endTime\": \"2025-12-31T23:59:59\"\n" +
                "}\n",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Thông tin tạo voucher toàn shop",
            content = @Content(
                schema = @Schema(implementation = ShopWideVoucherRequest.class)
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Voucher toàn shop đã được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc lỗi logic"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực")
        }
    )
    @PostMapping("/shop-wide")
    public ResponseEntity<BaseResponse> createShopWideVoucher(@RequestBody ShopWideVoucherRequest req) {
        return service.createShopWideVoucher(req);
    }

    // ============================================================
    // 📦 GET ALL VOUCHERS
    // ============================================================
    @Operation(
            summary = "Lấy tất cả voucher của cửa hàng hiện tại",
            description = """
                    Trả về danh sách tất cả voucher thuộc về cửa hàng đang đăng nhập.
                    Bao gồm cả voucher đang hoạt động, đã hết hạn hoặc bị vô hiệu hóa.
                    """
    )
    @GetMapping
    public ResponseEntity<BaseResponse> getAllVouchers() {
        return service.getAllVouchers();
    }

    // ============================================================
    // 🔍 GET VOUCHER BY ID
    // ============================================================
    @Operation(
            summary = "Xem chi tiết voucher theo ID",
            description = """
                    Lấy chi tiết voucher bao gồm thông tin cấu hình, điều kiện và danh sách sản phẩm được áp dụng.
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
    // 🚫 TOGGLE ENABLE / DISABLE
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

    @Operation(
        summary = "Lấy voucher ACTIVE của một sản phẩm",
        description = """
                Trả về thông tin voucher đang hoạt động (ACTIVE)
                được áp dụng cho sản phẩm có ID tương ứng.
                """
)
@GetMapping("/product/{productId}")
public ResponseEntity<BaseResponse> getVoucherByProduct(@PathVariable UUID productId) {
    return service.getActiveVoucherByProductId(productId);
}

/**
     * Lấy danh sách voucher theo trạng thái và loại scopeType.
     * Query: status (ACTIVE, DISABLED, ...), scopeType (PRODUCT_VOUCHER, ALL_SHOP_VOUCHER, null)
     * Nếu không truyền scopeType sẽ trả về tất cả theo trạng thái.
     * Ví dụ:
     *   /api/shop-vouchers/filter?status=ACTIVE&scopeType=ALL_SHOP_VOUCHER
     */
    @Operation(
        summary = "Lọc voucher theo trạng thái và loại voucher",
        description = "Lấy danh sách voucher theo trạng thái (ACTIVE, DISABLED, ...) và loại voucher (PRODUCT_VOUCHER, ALL_SHOP_VOUCHER).\n" +
                "- Query: status (bắt buộc), scopeType (tùy chọn: PRODUCT_VOUCHER, ALL_SHOP_VOUCHER).\n" +
                "- Nếu không truyền scopeType sẽ trả về tất cả voucher theo trạng thái.\n" +
                "\nVí dụ:\n" +
                "  /api/shop-vouchers/filter?status=ACTIVE&scopeType=ALL_SHOP_VOUCHER\n" +
                "  /api/shop-vouchers/filter?status=ACTIVE\n",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "status", description = "Trạng thái voucher (ACTIVE, DISABLED, ...)", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "scopeType", description = "Loại voucher: PRODUCT_VOUCHER (áp dụng cho sản phẩm), ALL_SHOP_VOUCHER (toàn shop)", required = false)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách voucher theo trạng thái và loại"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực")
        }
    )
    @GetMapping("/filter")
    public ResponseEntity<BaseResponse> getVouchersByStatusAndType(@RequestParam VoucherStatus status,
                                                                  @RequestParam(required = false) ShopVoucherScopeType scopeType) {
        return service.getActiveVouchersByType(status, scopeType);
    }

}
