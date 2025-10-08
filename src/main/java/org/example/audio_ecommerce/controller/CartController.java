package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AddToCartRequest;
import org.example.audio_ecommerce.dto.request.ToggleItemRequest;
import org.example.audio_ecommerce.dto.request.UpdateQtyRequest;
import org.example.audio_ecommerce.dto.response.CartSummaryResponse;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Validated
@Tag(name = "🛒 Cart API", description = "Các API quản lý giỏ hàng của người dùng (giống Shopee).")
public class CartController {

    private final CartService cartService;

    /**
     * Tạm thời dùng Header X-User-Id để lấy UUID người dùng,
     * trong thực tế sẽ lấy từ JWT (AuthenticationPrincipal).
     */
    private UUID ownerIdFromHeader(String id) {
        return UUID.fromString(id);
    }

    // ========================================================================
    // 1️⃣ LẤY GIỎ HÀNG HIỆN TẠI
    // ========================================================================
    @Operation(
            summary = "Lấy giỏ hàng hiện tại của người dùng",
            description = """
                    Lấy danh sách toàn bộ sản phẩm có trong giỏ hàng hiện tại.
                    Các sản phẩm được **group theo cửa hàng (store)** giống Shopee.
                    Trả về tổng số lượng sản phẩm đã tick chọn và tổng tiền tạm tính.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lấy giỏ hàng thành công",
                            content = @Content(schema = @Schema(implementation = CartSummaryResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
            }
    )
    @GetMapping
    public CartSummaryResponse getCart(
            @RequestHeader("X-User-Id") String userId
    ) {
        return cartService.getMyCart(ownerIdFromHeader(userId));
    }

    // ========================================================================
    // 2️⃣ THÊM SẢN PHẨM / COMBO VÀO GIỎ
    // ========================================================================
    @Operation(
            summary = "Thêm sản phẩm hoặc combo vào giỏ hàng",
            description = """
                    Dùng khi người dùng bấm nút **“Thêm vào giỏ hàng”** ở trang sản phẩm.
                    - Nếu giỏ hàng chưa tồn tại → hệ thống sẽ tự tạo mới.
                    - Nếu sản phẩm đã có → hệ thống cộng dồn số lượng.
                    - Chỉ truyền 1 trong 2: `productId` hoặc `comboId`.
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Thêm vào giỏ thành công",
                            content = @Content(schema = @Schema(implementation = CartSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Truyền sai dữ liệu"),
                    @ApiResponse(responseCode = "404", description = "Sản phẩm hoặc combo không tồn tại")
            }
    )
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartSummaryResponse addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest req
    ) {
        return cartService.addToCart(ownerIdFromHeader(userId), req);
    }

    // ========================================================================
    // 3️⃣ TICK CHỌN / BỎ CHỌN 1 ITEM
    // ========================================================================
//    @Operation(
//            summary = "Tick chọn hoặc bỏ chọn sản phẩm trong giỏ hàng",
//            description = """
//                    Dùng khi người dùng tick/untick checkbox ở giao diện giỏ hàng.
//                    - Những sản phẩm có `selected=true` sẽ được tính khi checkout.
//                    - Giống hành vi “Chọn sản phẩm muốn mua” trên Shopee.
//                    """,
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
//                            content = @Content(schema = @Schema(implementation = CartSummaryResponse.class))),
//                    @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ")
//            }
//    )
//    @PatchMapping("/items/{itemId}/toggle")
//    public CartSummaryResponse toggle(
//            @RequestHeader("X-User-Id") String userId,
//            @PathVariable UUID itemId,
//            @Valid @RequestBody ToggleItemRequest req
//    ) {
//        return cartService.toggleItem(ownerIdFromHeader(userId), itemId, Boolean.TRUE.equals(req.getSelected()));
//    }

    // ========================================================================
    // 4️⃣ CẬP NHẬT SỐ LƯỢNG
    // ========================================================================
    @Operation(
            summary = "Cập nhật số lượng sản phẩm trong giỏ hàng",
            description = """
                    Khi người dùng nhấn nút “+” hoặc “–” để tăng/giảm số lượng.
                    - Không cho phép số lượng <= 0.
                    - Tự động cập nhật lại `subtotal` (unitPrice * quantity).
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                            content = @Content(schema = @Schema(implementation = CartSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Số lượng không hợp lệ"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ")
            }
    )
    @PatchMapping("/items/{itemId}/quantity")
    public CartSummaryResponse updateQty(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateQtyRequest req
    ) {
        return cartService.updateQuantity(ownerIdFromHeader(userId), itemId, req.getQuantity());
    }

    // ========================================================================
    // 5️⃣ XÓA ITEM KHỎI GIỎ
    // ========================================================================
    @Operation(
            summary = "Xóa sản phẩm khỏi giỏ hàng",
            description = """
                    Khi người dùng nhấn nút “🗑️ Xóa” trên giao diện giỏ hàng.
                    - Xóa hoàn toàn sản phẩm hoặc combo khỏi giỏ.
                    - Sau khi xóa, hệ thống trả về giỏ hàng còn lại.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Xóa thành công",
                            content = @Content(schema = @Schema(implementation = CartSummaryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ")
            }
    )
    @DeleteMapping("/items/{itemId}")
    public CartSummaryResponse remove(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId
    ) {
        return cartService.removeItem(ownerIdFromHeader(userId), itemId);
    }
}
