package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            summary = "Lấy giỏ hàng hiện tại",
            description = "Trả về giỏ hàng ACTIVE của khách hàng (nếu chưa có có thể trả về giỏ rỗng)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy customer")
    })
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

    @Operation(
            summary = "Pre-check COD eligibility cho danh sách item sẽ checkout",
            description = "Nhận danh sách CheckoutItemRequest[] và trả về `overallEligible` cùng chi tiết từng store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = CodEligibilityResponse.class)))
    })
    @PostMapping("/cod-eligibility")
    public ResponseEntity<CodEligibilityResponse> checkCodEligibility(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Array CheckoutItemRequest[]. Ví dụ: [{\"type\":\"PRODUCT\",\"id\":\"...\"}]",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CheckoutItemRequest.class)))
            )
            @RequestBody List<CheckoutItemRequest> items
    ) {
        CodEligibilityResponse res = cartService.checkCodEligibility(customerId, items);
        return ResponseEntity.ok(res);
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

    // ví dụ trong CartController (tùy bạn đặt)
    @PostMapping("/checkout/store-ship")
    public ResponseEntity<List<CustomerOrderResponse>> checkoutStoreShip(
            @RequestParam UUID customerId,
            @RequestBody CheckoutCODRequest request // tái dùng request hiện có: items, addressId, message, vouchers
    ) {
        List<CustomerOrderResponse> resp = cartService.checkoutStoreShip(customerId, request);
        return ResponseEntity.ok(resp);
    }

}
