package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cart", description = "Các API thao tác giỏ hàng của khách hàng")
@RestController
@RequestMapping("/api/v1/customers/{customerId}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(
            summary = "Lấy giỏ hàng hiện tại của Customer",
            description = """
  API trả về **giỏ hàng ACTIVE** hiện tại của customer để FE hiển thị màn Cart.

  ======================
  1) FE HIỂN THỊ CẦN GÌ?
  ======================
  - Render `items[]`: tên, ảnh, variant, origin, quantity.
  - Render giá từng item theo đúng BE:
    - `unitPrice`: đơn giá hiệu lực (giá BE đang áp dụng).
    - `lineTotal`: = unitPrice * quantity (BE trả sẵn).
  - Render tổng tiền cart:
    - `subtotal`: tổng lineTotal
    - `discountTotal`: tổng giảm (cấp cart)
    - `grandTotal`: = subtotal - discountTotal

  ======================
  2) QUY TẮC HIỂN THỊ GIÁ (RẤT QUAN TRỌNG)
  ======================
  Các field liên quan giá:
  - `baseUnitPrice`: giá gốc (chưa áp campaign)
  - `platformCampaignPrice`: giá sau campaign (nếu có)
  - `inPlatformCampaign`: item có thuộc campaign hay không
  - `campaignUsageExceeded`: customer đã dùng vượt limit campaign hay chưa
  - `campaignRemaining`: số lượt còn lại (nếu có giới hạn)

  Rule FE khuyến nghị:
  - Nếu `inPlatformCampaign = true` và `platformCampaignPrice != null` và `campaignUsageExceeded != true`:
    -> Giá bán hiển thị = `platformCampaignPrice`
    -> Giá gạch/compare = `baseUnitPrice` (nếu != null)
  - Nếu `campaignUsageExceeded = true`:
    -> Hiển thị badge “Hết lượt ưu đãi”
    -> Không dùng `platformCampaignPrice` (coi như không có campaign cho user này)
  - Nếu không có campaign:
    -> Hiển thị giá bán = `unitPrice`
    -> Giá gạch chỉ hiển thị nếu FE có rule khác (tùy UI)

  ======================
  3) VARIANT / ORIGIN
  ======================
  - Variant:
    - Nếu `variantId != null`: hiển thị `variantOptionName : variantOptionValue`
    - Ưu tiên ảnh `variantUrl` nếu có (fallback về `image`)
  - Origin (nguồn gửi):
    - `originProvinceCode`, `originDistrictCode`, `originWardCode`
    - FE dùng để hiển thị “Gửi từ …” và phục vụ tính ship ở bước preview checkout

  ======================
  4) LƯU Ý
  ======================
  - API này KHÔNG tạo đơn hàng, KHÔNG trừ tiền, KHÔNG giữ tồn kho.
  - Chỉ dùng để hiển thị giỏ hàng.
  """
    )
    @ApiResponse(responseCode = "200", description = "OK - Trả về CartResponse")
    @ApiResponse(responseCode = "400", description = "Sai định dạng UUID hoặc tham số không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy customer hoặc giỏ hàng")
    @GetMapping
    public CartResponse getActive(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId) {
        return cartService.getActiveCart(customerId);
    }

    @Operation(
            summary = "Thêm nhiều item vào giỏ hàng",
            description = "Cho phép thêm nhiều sản phẩm (PRODUCT) hoặc combo (COMBO) vào giỏ hàng. "
                    + "Nếu item đã tồn tại thì sẽ được cộng dồn số lượng."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Thêm thành công",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy customer / product / combo")
    })
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CartResponse> addItems(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCartItemsRequest req) {
        CartResponse cart = cartService.addItems(customerId, req);
        return BaseResponse.success("✅ Thêm vào giỏ hàng thành công", cart);
    }

    @Operation(
            summary = "Checkout COD các sản phẩm/combo được chọn trong giỏ hàng",
            description = """
                    Thanh toán COD: tạo CustomerOrder và tách StoreOrder theo từng cửa hàng.
                    - Body gồm danh sách items (PRODUCT/COMBO) và addressId (tuỳ chọn).
                    - Nếu không truyền addressId, hệ thống dùng địa chỉ mặc định của customer.
                    Trả về: id, status, createdAt, totalAmount và snapshot địa chỉ giao hàng.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout COD thành công",
                    content = @Content(schema = @Schema(implementation = CustomerOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi khi checkout COD")
    })
    @PostMapping("/checkout-cod")
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<List<CustomerOrderResponse>> checkoutCod(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Danh sách item cần checkout, addressId (tùy chọn) và voucher theo shop.
                            
                            - Nếu sản phẩm KHÔNG có biến thể: bỏ qua `variantUnitPrice` hoặc để null.
                            - Nếu CÓ biến thể: FE gửi `variantUnitPrice` = giá của biến thể đã chọn, BE sẽ dùng làm giá gốc.
                            
                            Ví dụ:
                            {
                              "items": [
                                {
                                  "type": "PRODUCT",
                                  "id": "product-uuid...",
                                  "quantity": 2,
                                  "variantUnitPrice": 3490000
                                },
                                {
                                  "type": "COMBO",
                                  "id": "combo-uuid...",
                                  "quantity": 1
                                }
                              ],
                              "addressId": "address-uuid...",
                              "storeVouchers": [
                                {
                                  "storeId": "11111111-1111-1111-1111-111111111111",
                                  "codes": ["SALE10K", "P10"]
                                }
                              ]
                            }
                            """,
                    required = true
            )

            @RequestBody CheckoutCODRequest request
    ) {
        List<CustomerOrderResponse> resp = cartService.checkoutCODWithResponse(customerId, request);
        return BaseResponse.success("✅ Checkout COD thành công", resp);
    }

    @Operation(summary = "Cập nhật số lượng của một item trong giỏ hàng",
            description = "Chỉ cập nhật 1 item cụ thể theo type + id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công", content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy item")
    })
    @PatchMapping("/item/quantity")
    public CartResponse updateItemQuantity(
            @Parameter(description = "ID khách hàng (UUID)", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCartItemQtyRequest req) {
        return cartService.updateItemQuantity(customerId, req);
    }

    @Operation(summary = "Xóa nhiều item khỏi giỏ hàng",
            description = "Nhận danh sách item cần xóa (type + id).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công", content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @DeleteMapping("/items")
    public CartResponse removeItems(
            @Parameter(description = "ID khách hàng (UUID)", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody RemoveCartItemRequest req) {
        return cartService.removeItems(customerId, req);
    }

    @Operation(summary = "Xóa toàn bộ giỏ hàng của khách hàng",
            description = "Xóa tất cả item trong giỏ hàng ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công", content = @Content(schema = @Schema(implementation = CartResponse.class)))
    })
    @DeleteMapping
    public CartResponse clearCart(
            @Parameter(description = "ID khách hàng (UUID)", required = true) @PathVariable UUID customerId) {
        return cartService.clearCart(customerId);
    }

    @Operation(summary = "Cập nhật số lượng nhiều item cùng lúc",
            description = "Dùng để đồng bộ giỏ hàng từ frontend (ví dụ: sau khi người dùng chỉnh sửa nhiều item).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công", content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PatchMapping("/items/bulk-quantity")
    public CartResponse bulkUpdateQuantities(
            @Parameter(description = "ID khách hàng (UUID)", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody BulkUpdateCartQtyRequest req) {
        return cartService.bulkUpdateQuantities(customerId, req);
    }

    @Operation(
            summary = "Cập nhật số lượng 1 item kèm tính lại voucher/platform campaign",
            description = """
                Dùng cho nút cộng/trừ số lượng trong giỏ hàng.
                - Body bao gồm: cartItemId, quantity mới, danh sách storeVouchers, platformVouchers.
                - BE sẽ:
                  + Cập nhật quantity và unitPrice (đã kiểm tra usage_per_user của campaign).
                  + Tính lại tổng giỏ hàng + discount từ voucher.
                  + Trả về CartResponse, trong đó mỗi item có thể có flag campaignUsageExceeded để FE hiển thị cảnh báo.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy customer / cartItem")
    })
    @PostMapping("/items/quantity-with-vouchers")
    public CartResponse updateItemQuantityWithVouchers(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCartItemQtyWithVoucherRequest request
    ) {
        return cartService.updateItemQuantityWithVouchers(customerId, request);
    }

    @Operation(
            summary = "Preview checkout (COD) từ giỏ hàng (chưa tạo đơn)",
            description = """
  API dùng để **xem trước thông tin checkout** trước khi tạo đơn thực tế.
  FE gọi ở màn hình “Xác nhận đặt hàng” để hiển thị breakdown: tiền hàng / phí ship / giảm giá / tổng phải trả.

  ======================
  1) REQUEST (CheckoutCODRequest)
  ======================
  - `items[]`: danh sách item muốn mua + số lượng chốt ở bước checkout
    - FE phải gửi đúng quantity user chọn mua (không phụ thuộc quantity đang lưu trong cart).
  - `addressId` (optional):
    - Nếu có: BE tính phí ship theo địa chỉ này.
    - Nếu null: BE có thể dùng địa chỉ mặc định (tuỳ logic hiện tại của bạn).
  - `message` (optional): ghi chú đơn (nếu có).
  - `storeVouchers` (optional): voucher theo shop.
  - `platformVouchers` (optional): voucher của sàn.
  - `serviceTypeIds` (optional Map<storeId, serviceTypeId>):
    - Cho phép FE chọn loại dịch vụ vận chuyển cho từng shop khi preview.

  ======================
  2) RESPONSE (CheckoutPreviewResponse)
  ======================
  Tổng quan toàn hệ thống (tất cả store):
  - `overallSubtotal`: tổng tiền hàng trước giảm (tổng linePriceBeforeDiscount)
  - `overallShipping`: tổng phí ship (cộng tất cả store)
  - `overallDiscount`: tổng giảm (platform + store)
  - `overallGrandTotal`: tổng phải trả = overallSubtotal + overallShipping - overallDiscount

  Breakdown theo từng store (`stores[]`):
  - `subtotal`: tổng tiền hàng trước giảm của store
  - `shippingFee`: phí ship của store
  - `platformDiscount`: giảm từ voucher sàn áp vào store (nếu có)
  - `storeDiscount`: giảm từ voucher shop (nếu có)
  - `discountTotal`: tổng giảm của store = platformDiscount + storeDiscount
  - `grandTotal`: tổng phải trả của store = subtotal + shippingFee - discountTotal
  - `shippingServiceTypeId` (optional): loại dịch vụ ship BE đã dùng/tính cho store
  - `storeVoucherDetailJson`, `platformVoucherDetailJson` (optional):
    - JSON string để FE hiển thị chi tiết voucher áp dụng (tên, code, số tiền giảm, điều kiện...)

  Item breakdown trong từng store (`stores[].items[]`):
  - `unitPriceBeforeDiscount`: đơn giá trước voucher (và trước các giảm cấp store/platform)
  - `linePriceBeforeDiscount`: = unitPriceBeforeDiscount * quantity
  - `finalUnitPrice`: đơn giá sau khi phân bổ/áp giảm (nếu có)
  - `finalLineTotal`: = finalUnitPrice * quantity

  ======================
  3) QUY TẮC HIỂN THỊ FE (KHUYẾN NGHỊ)
  ======================
  - Hiển thị tổng tiền trang checkout theo:
    - Tiền hàng: overallSubtotal
    - Phí ship: overallShipping
    - Giảm giá: overallDiscount
    - Tổng thanh toán: overallGrandTotal
  - Nếu UI chia theo shop:
    - Render stores[] và dùng PerStore.subtotal/shippingFee/discountTotal/grandTotal.
  - Giá item hiển thị:
    - Giá gốc: unitPriceBeforeDiscount
    - Giá sau giảm: finalUnitPrice (nếu khác null và khác giá gốc)
    - Tổng dòng: finalLineTotal

  ======================
  4) LƯU Ý QUAN TRỌNG
  ======================
  - Preview KHÔNG tạo CustomerOrder/StoreOrder.
  - KHÔNG trừ tiền, KHÔNG giữ tồn kho.
  - Có thể trả lỗi nếu:
    - quantity không hợp lệ
    - item không thuộc cart/customer
    - hết hàng / không đủ tồn
    - voucher không hợp lệ / không áp dụng
    - thiếu addressId trong trường hợp bắt buộc để tính ship (tuỳ rule của bạn)
  """
    )
    @ApiResponse(responseCode = "200", description = "OK - Trả về CheckoutPreviewResponse")
    @ApiResponse(responseCode = "400", description = "Request body không hợp lệ / quantity <= 0 / thiếu items")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy customer/cart/cartItem/address")
    @ApiResponse(responseCode = "409", description = "Hết hàng / voucher không áp dụng / dữ liệu thay đổi gây xung đột")
    @PostMapping("/checkout/preview")
    public BaseResponse<CheckoutPreviewResponse> previewCheckout(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @Valid @RequestBody CheckoutCODRequest request
    ) {
        return BaseResponse.success("Preview checkout successful",cartService.previewCheckout(customerId, request));
    }
}
