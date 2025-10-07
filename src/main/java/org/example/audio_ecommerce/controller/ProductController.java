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

    @Operation(summary = "➕ Tạo sản phẩm mới")
    @PostMapping
    public ResponseEntity<BaseResponse> createProduct(@RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @Operation(summary = "✏️ Cập nhật sản phẩm")
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> updateProduct(
            @PathVariable UUID productId,
            @RequestBody ProductRequest request
    ) {
        return productService.updateProduct(productId, request);
    }

    @Operation(summary = "🚫 Vô hiệu hóa sản phẩm (không xóa DB)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<BaseResponse> disableProduct(@PathVariable UUID productId) {
        return productService.disableProduct(productId);
    }
}
