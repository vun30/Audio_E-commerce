package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

public interface StoreService {

    ResponseEntity<BaseResponse> getStoreById(UUID storeId);

    ResponseEntity<BaseResponse> getStoreByAccountId(UUID accountId);

    ResponseEntity<BaseResponse> updateStore(UUID storeId, UpdateStoreRequest request);

    ResponseEntity<BaseResponse> updateStoreStatus(UUID storeId, StoreStatus status);

    ResponseEntity<BaseResponse> getAllStores(int page, int size, String keyword);

    Optional<Store> getStoreByEmail(String email);
}
