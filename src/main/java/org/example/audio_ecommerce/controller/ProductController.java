package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Product", description = "📦 API quản lý sản phẩm (Admin & Store)")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ============================================================
    // 📜 Lấy danh sách sản phẩm
    // ============================================================
     @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm (lọc theo danh mục, store, keyword, status)")
    public ResponseEntity<BaseResponse> getAllProducts(
            @Parameter(
                description = "Tên danh mục (chọn từ dropdown)",
                schema = @Schema(
                        allowableValues = {
                                "Tai Nghe", "Loa", "Micro", "DAC", "Mixer",
                                "Amp", "Turntable", "Sound Card", "DJ Controller", "Combo"
                        }
                )
            )
            @RequestParam(required = false) String categoryName,

            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ProductStatus status
    ) {
        return productService.getAllProducts(categoryName, storeId, keyword, page, size, status);
    }

    // ============================================================
    // 🔎 Lấy chi tiết sản phẩm
    // ============================================================
    @Operation(summary = "🔎 Lấy chi tiết sản phẩm theo ID")
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse> getProductById(@PathVariable UUID productId) {
        return productService.getProductById(productId);
    }

    // ============================================================
    // ➕ Tạo sản phẩm mới
    // ============================================================
    @Operation(
        summary = "➕ Tạo sản phẩm mới (Store)",
        description = """
        • API cho phép **Store** tạo sản phẩm mới lên sàn.  
        • `categoryName` chọn 1 trong các giá trị: **Loa**, **Tai Nghe**, **Micro**, **DAC**, **Mixer**, **Amp**, **Turntable**, **Sound Card**, **DJ Controller**, **Combo**.  
        • `storeId` được tự động xác định từ tài khoản đăng nhập.  
        • `slug` sinh tự động từ `name`.  
        • `sku` phải duy nhất trong mỗi cửa hàng.  
        • Các trường giá gồm:
          - `promotionPercent`: % khuyến mãi riêng
          - `platformFeePercent`: % phí sàn
          - `bulkDiscounts`: danh sách khoảng giá mua nhiều (tùy chọn)
        • Kết quả trả về: thông tin sản phẩm chi tiết.
        """
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createProduct(@RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    // ============================================================
    // ✏️ Cập nhật sản phẩm
    // ============================================================
    @Operation(
        summary = "✏️ Cập nhật sản phẩm (Store)",
        description = """
        • Cho phép **Store** cập nhật sản phẩm của mình.  
        • Có thể thay đổi: 
          - `name`, `slug` sẽ tự sinh lại.
          - `categoryName` → BE tự map sang `categoryId`.
          - `bulkDiscounts` → BE tự cập nhật danh sách mức giá mua nhiều.
        • Nếu trường nào không nhập → giữ nguyên giá trị cũ.
        • Kết quả: sản phẩm sau khi cập nhật thành công.
        """
    )
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> updateProduct(
        @PathVariable UUID productId,
        @RequestBody ProductRequest request
) {
    return productService.updateProduct(productId, request);
}

    // ============================================================
    // 🚫 Vô hiệu hóa / Kích hoạt sản phẩm
    // ============================================================
    @Operation(
        summary = "🚫 Vô hiệu hóa hoặc kích hoạt sản phẩm",
        description = """
        • API này **không xóa sản phẩm khỏi DB**.  
        • Chỉ đổi trạng thái giữa:
          - **ACTIVE** ↔ **DISCONTINUED**.  
        • Dùng khi muốn ẩn tạm thời sản phẩm khỏi gian hàng.
        """
    )
    @DeleteMapping("/{productId}")
    public ResponseEntity<BaseResponse> disableProduct(@PathVariable UUID productId) {
        return productService.disableProduct(productId);
    }
}
