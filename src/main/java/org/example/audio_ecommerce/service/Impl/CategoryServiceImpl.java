package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.example.audio_ecommerce.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public ResponseEntity<BaseResponse> getAllCategories(String keyword) {
        List<Category> categories = (keyword != null && !keyword.isBlank())
                ? categoryRepository.findByNameContainingIgnoreCase(keyword)
                : categoryRepository.findAllByOrderBySortOrderAsc();

        return ResponseEntity.ok(new BaseResponse<>(200, "Danh sách category", categories));
    }

    @Override
    public ResponseEntity<BaseResponse> getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        return ResponseEntity.ok(new BaseResponse<>(200, "Chi tiết category", category));
    }

    @Override
    public ResponseEntity<BaseResponse> createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Danh mục đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .sortOrder(request.getSortOrder())
                .build();

        categoryRepository.save(category);
        return ResponseEntity.ok(new BaseResponse<>(201, "Tạo danh mục thành công", category));
    }

    @Override
    public ResponseEntity<BaseResponse> updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getSlug() != null) category.setSlug(request.getSlug());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getIconUrl() != null) category.setIconUrl(request.getIconUrl());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());

        categoryRepository.save(category);
        return ResponseEntity.ok(new BaseResponse<>(200, "Cập nhật danh mục thành công", category));
    }

    @Override
    public ResponseEntity<BaseResponse> deleteCategory(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Không tìm thấy danh mục");
        }
        categoryRepository.deleteById(categoryId);
        return ResponseEntity.ok(new BaseResponse<>(200, "Xóa danh mục thành công", null));
    }
}
