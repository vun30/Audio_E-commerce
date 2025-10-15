package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
            @RequestBody(
                    required = true,
                    description = "Danh sách item cần thêm",
                    content = @Content(schema = @Schema(implementation = AddCartItemsRequest.class),
                            examples = @ExampleObject(name = "Add Product + Combo", value = """
                                {
                                  "items": [
                                    { "type": "PRODUCT", "id": "68d41bfd-0c99-cc31-eb55-11111111", "quantity": 2 },
                                    { "type": "COMBO", "id": "77c41bfd-0c99-cc31-eb55-22222222", "quantity": 1 }
                                  ]
                                }
                                """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AddCartItemsRequest req) {
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
}
