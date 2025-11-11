package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.SellBannerCreateRequest;
import org.example.audio_ecommerce.dto.request.SellBannerUpdateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface SellBannerService {
    ResponseEntity<BaseResponse> createBanner(SellBannerCreateRequest req);
    ResponseEntity<BaseResponse> updateBanner(UUID id, SellBannerUpdateRequest req);
    ResponseEntity<BaseResponse> getAllBanners(Boolean active);
    ResponseEntity<BaseResponse> getBannerById(UUID id);
    ResponseEntity<BaseResponse> deleteBanner(UUID id);
}
