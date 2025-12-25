package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ProductSpecUpsertRequest;
import org.example.audio_ecommerce.dto.request.SimilarProductRequest;

import java.util.List;
import java.util.UUID;

public interface ProductSpecService {

    void upsertSpec(
            UUID productId,
            UUID categoryId,
            String categoryName,
            ProductSpecUpsertRequest request
    );

    List<UUID> searchSimilar(SimilarProductRequest request);
}