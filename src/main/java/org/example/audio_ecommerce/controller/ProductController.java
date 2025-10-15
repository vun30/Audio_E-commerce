package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
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

    @Operation(summary = "📜 Lấy danh sách sản phẩm (phân trang + lọc)")
@GetMapping
public ResponseEntity<BaseResponse> getAllProducts(
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID storeId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) ProductStatus status // ✅ thêm vào
) {
    return productService.getAllProducts(categoryId, storeId, keyword, page, size, status);
}

    @Operation(summary = "🔎 Lấy chi tiết sản phẩm")
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse> getProductById(@PathVariable UUID productId) {
        return productService.getProductById(productId);
    }

    @Operation(
    summary = "➕ Tạo sản phẩm mới (Store)",
    description = """
    • API cho phép **Store** tạo sản phẩm mới lên sàn.  
    • `categoryName` chọn 1 trong các giá trị: **Loa**, **Micro**, **Turntable**, **Mixer**, **Amp**, **DJ Controller**, **Sound Card**, **DAC**, **Combo**.  
    • `storeId` được lấy tự động từ tài khoản đang đăng nhập.  
    • `slug` được sinh tự động từ `name`.  
    • `sku` phải duy nhất trong mỗi cửa hàng.  
    • Tất cả các trường khác là tùy chọn (có thể null).  
    • Kết quả trả về: thông tin chi tiết sản phẩm vừa được tạo.
    """
)
    @PostMapping
    public ResponseEntity<BaseResponse> createProduct(@RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @Operation(
    summary = "✏️ Cập nhật thông tin sản phẩm (Store)",
    description = """
    • Cho phép **Store** cập nhật thông tin sản phẩm của mình.  
    • Có thể đổi `categoryName`, hệ thống sẽ tự tìm `categoryId` tương ứng trong DB.  
    • Có thể đổi `name`, `slug` sẽ tự sinh lại.  
    • `sku` có thể đổi, nhưng phải **duy nhất trong store**.  
    • Các trường khác nếu để trống sẽ **giữ nguyên giá trị cũ**.  
    • Kết quả trả về: sản phẩm sau khi cập nhật thành công.
    """
)
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> updateProduct(
            @PathVariable UUID productId,
            @RequestBody ProductRequest request
    ) {
        return productService.updateProduct(productId, request);
    }

    @Operation(
    summary = "🚫 Vô hiệu hóa hoặc kích hoạt lại sản phẩm",
    description = """
    • API này **không xóa sản phẩm khỏi DB**.  
    • Thay vào đó, chỉ đổi trạng thái giữa:
      - **ACTIVE** ↔ **DISCONTINUED**.  
    • Dùng khi muốn ẩn tạm thời sản phẩm khỏi gian hàng.  
    • Kết quả trả về sản phẩm với trạng thái mới.
    """
)
    @DeleteMapping("/{productId}")
    public ResponseEntity<BaseResponse> disableProduct(@PathVariable UUID productId) {
        return productService.disableProduct(productId);
    }
}
