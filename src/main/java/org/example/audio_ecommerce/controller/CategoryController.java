package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Category", description = "API CRUD danh mục sản phẩm (Admin)")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "📜 Lấy tất cả danh mục", description = "Hỗ trợ tìm kiếm theo tên nếu truyền `keyword`.")
    @GetMapping
    public ResponseEntity<BaseResponse> getAllCategories(
            @RequestParam(required = false) String keyword
    ) {
        return categoryService.getAllCategories(keyword);
    }

    @Operation(summary = "🔎 Lấy chi tiết danh mục")
    @GetMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> getCategoryById(@PathVariable UUID categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @Operation(summary = "➕ Tạo danh mục mới")
    @PostMapping
    public ResponseEntity<BaseResponse> createCategory(@RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @Operation(summary = "✏️ Cập nhật danh mục")
    @PutMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> updateCategory(
            @PathVariable UUID categoryId,
            @RequestBody CategoryRequest request
    ) {
        return categoryService.updateCategory(categoryId, request);
    }

    @Operation(summary = "🗑️ Xóa danh mục")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> deleteCategory(@PathVariable UUID categoryId) {
        return categoryService.deleteCategory(categoryId);
    }
}
