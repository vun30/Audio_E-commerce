package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "Product Combo", description = "📦 API quản lý combo sản phẩm - tạo, cập nhật, lọc, vô hiệu hóa, và xem chi tiết combo.")
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService productComboService;

    @Operation(
            summary = "➕ Tạo combo sản phẩm",
            description = "Tạo combo mới từ một product chính (comboProductId) và danh sách các sản phẩm con. **Lưu ý:** tất cả sản phẩm phải thuộc cùng 1 cửa hàng, nếu không sẽ báo lỗi."
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createCombo(@RequestBody CreateComboRequest request) {
        return productComboService.createCombo(request);
    }

    @Operation(
            summary = "🔎 Lấy chi tiết combo",
            description = "Nhập `comboId` để lấy thông tin chi tiết combo, bao gồm sản phẩm chính, mô tả, giá, danh mục và danh sách ID các sản phẩm trong combo."
    )
    @GetMapping("/{comboId}")
    public ResponseEntity<BaseResponse> getComboById(@PathVariable UUID comboId) {
        return productComboService.getComboById(comboId);
    }

    @Operation(
            summary = "📜 Lấy danh sách combo (toàn hệ thống)",
            description = """
                    Lấy danh sách combo có hỗ trợ **lọc và phân trang**:
                    - `keyword`: tìm theo tên combo
                    - `sortDir`: asc (giá tăng dần) / desc (giá giảm dần)
                    - `minPrice`, `maxPrice`: lọc theo khoảng giá
                    - `page`, `size`: phân trang
                    """
    )
    @GetMapping
    public ResponseEntity<BaseResponse> getAllCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return productComboService.getAllCombos(page, size, keyword, sortDir, minPrice, maxPrice);
    }

    @Operation(
            summary = "🏪 Lấy combo theo store ID",
            description = """
                    Lấy danh sách combo thuộc một cửa hàng cụ thể.
                    Hỗ trợ bộ lọc và phân trang giống như API lấy tất cả combo.
                    - `storeId`: ID cửa hàng cần lọc
                    """
    )
    @GetMapping("/store/{storeId}")
    public ResponseEntity<BaseResponse> getCombosByStoreId(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return productComboService.getCombosByStoreId(storeId, page, size, keyword, sortDir, minPrice, maxPrice);
    }

    @Operation(
            summary = "✏️ Cập nhật combo",
            description = "Cập nhật thông tin combo (ảnh, mô tả, giá, danh mục, danh sách sản phẩm...). Nếu danh sách sản phẩm thay đổi, hệ thống sẽ tự động tính lại giá gốc (`originalTotalPrice`)."
    )
    @PutMapping("/{comboId}")
    public ResponseEntity<BaseResponse> updateCombo(
            @PathVariable UUID comboId,
            @RequestBody UpdateComboRequest request
    ) {
        return productComboService.updateCombo(comboId, request);
    }

    @Operation(
            summary = "🚫 Vô hiệu hóa combo",
            description = "Thay vì xóa hẳn khỏi DB, combo sẽ được đánh dấu `isActive = false` và không hiển thị trên trang bán hàng."
    )
    @PutMapping("/{comboId}/disable")
    public ResponseEntity<BaseResponse> disableCombo(@PathVariable UUID comboId) {
        return productComboService.disableCombo(comboId);
    }

    @Operation(
            summary = "📦 Lấy danh sách sản phẩm trong combo",
            description = "Nhập `comboId` để lấy toàn bộ chi tiết các sản phẩm con trong combo (ID, tên, giá, ảnh, danh mục...)."
    )
    @GetMapping("/{comboId}/products")
    public ResponseEntity<BaseResponse> getProductsInCombo(@PathVariable UUID comboId) {
        return productComboService.getProductsInCombo(comboId);
    }
}
