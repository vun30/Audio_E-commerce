package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductViewService {

    /**
     * Lấy danh sách sản phẩm thumbnail có lọc và phân trang
     */
    ResponseEntity<BaseResponse> getThumbnailView(
            String status,
            UUID categoryId,
            UUID storeId,
            String keyword,
            String provinceCode,
            String districtCode,
            String wardCode,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            Pageable pageable
    );

    ResponseEntity<BaseResponse> getActiveVouchersOfProduct(UUID productId, String type, String campaignType);


}
