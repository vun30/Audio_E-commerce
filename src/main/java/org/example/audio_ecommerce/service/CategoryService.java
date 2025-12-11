package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CreateCategoryRequest;
import org.example.audio_ecommerce.dto.request.UpdateCategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface CategoryService {

    ResponseEntity<BaseResponse> createCategory(CreateCategoryRequest req);

    ResponseEntity<BaseResponse> updateCategory(UUID categoryId, UpdateCategoryRequest req);

    ResponseEntity<BaseResponse> deleteCategory(UUID categoryId);

    ResponseEntity<BaseResponse> getCategory(UUID categoryId);

    ResponseEntity<BaseResponse> getCategoryTree();
}
