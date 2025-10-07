package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface CategoryService {
    ResponseEntity<BaseResponse> getAllCategories(String keyword);
    ResponseEntity<BaseResponse> getCategoryById(UUID categoryId);
    ResponseEntity<BaseResponse> createCategory(CategoryRequest request);
    ResponseEntity<BaseResponse> updateCategory(UUID categoryId, CategoryRequest request);
    ResponseEntity<BaseResponse> deleteCategory(UUID categoryId);
}
