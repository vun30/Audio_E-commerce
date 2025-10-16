package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.request.CheckoutItemRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

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
    public CartResponse addItems(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCartItemsRequest req) {
        return cartService.addItems(customerId, req);
    }

    @Operation(
            summary = "Checkout giỏ hàng",
            description = "Khách hàng tiến hành checkout giỏ hàng, tạo đơn hàng mới, đồng thời ghi transaction vào ví customer và ví web (platform)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout thành công"),
            @ApiResponse(responseCode = "400", description = "Giỏ hàng rỗng, số dư không đủ hoặc lỗi khác")
    })
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.OK)
    public void checkout(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId
    ) {
        cartService.checkout(customerId);
    }

    @Operation(
            summary = "Checkout các sản phẩm/combo được chọn trong giỏ hàng",
            description = "Thanh toán các sản phẩm hoặc combo được chọn trong giỏ hàng của khách hàng.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Danh sách sản phẩm/combo muốn checkout",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CheckoutItemRequest.class)))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi checkout")
    })
    @PostMapping("/checkout-selected")
    @ResponseStatus(HttpStatus.OK)
    public void checkoutSelected(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody List<CheckoutItemRequest> items
    ) {
        cartService.checkout(customerId, items);
    }

    @Operation(
        summary = "Checkout COD các sản phẩm/combo được chọn trong giỏ hàng",
        description = "Thanh toán COD: tạo order cho customer và tách order cho từng store. Trả về id và trạng thái order cho customer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout COD thành công"),
        @ApiResponse(responseCode = "400", description = "Lỗi khi checkout COD")
    })
    @PostMapping("/checkout-cod")
    @ResponseStatus(HttpStatus.OK)
    public CustomerOrderResponse checkoutCod(
            @Parameter(description = "ID khách hàng (UUID)", required = true)
            @PathVariable UUID customerId,
            @RequestBody List<CheckoutItemRequest> items
    ) {
        return cartService.checkoutCODWithResponse(customerId, items);
    }
}
