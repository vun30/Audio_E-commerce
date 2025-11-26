package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {
    ResponseEntity<BaseResponse> createProduct(ProductRequest request);
    ResponseEntity<BaseResponse> getAllProducts(
            String categoryName,  // ⚡ đổi từ UUID categoryId -> String categoryName
            UUID storeId,
            String keyword,
            int page,
            int size,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
);
    ResponseEntity<BaseResponse> getProductById(UUID productId);
    ResponseEntity<BaseResponse> updateProduct(UUID productId, UpdateProductRequest request);
    ResponseEntity<BaseResponse> disableProduct(UUID productId);
}
