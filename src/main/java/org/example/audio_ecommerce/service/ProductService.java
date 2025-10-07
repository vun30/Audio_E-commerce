package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ProductService {
    ResponseEntity<BaseResponse> createProduct(ProductRequest request);
    ResponseEntity<BaseResponse> getAllProducts(
        UUID categoryId,
        UUID storeId,
        String keyword,
        int page,
        int size,
        ProductStatus status
);
    ResponseEntity<BaseResponse> getProductById(UUID productId);
    ResponseEntity<BaseResponse> updateProduct(UUID productId, ProductRequest request);
    ResponseEntity<BaseResponse> disableProduct(UUID productId);
}
