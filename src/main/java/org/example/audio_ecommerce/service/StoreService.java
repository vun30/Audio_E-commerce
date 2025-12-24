package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest.StoreAddressRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

public interface StoreService {

    // =========================================================
    // 🏪 STORE CRUD
    // =========================================================
    ResponseEntity<BaseResponse> getStoreById(UUID storeId);

    ResponseEntity<BaseResponse> getStoreByAccountId(UUID accountId);

    ResponseEntity<BaseResponse> updateStore(UUID storeId, UpdateStoreRequest request);

    ResponseEntity<BaseResponse> updateStoreStatus(UUID storeId, StoreStatus status, String reason);


    ResponseEntity<BaseResponse> getAllStores(int page, int size, String keyword);

    Optional<Store> getStoreByEmail(String email);


    // =========================================================
    // 🏠 STORE ADDRESS CRUD (dùng UUID)
    // =========================================================

    /**
     * 📋 Lấy tất cả địa chỉ của cửa hàng (theo user đang đăng nhập)
     */
    ResponseEntity<BaseResponse> getAllAddresses();

    /**
     * ➕ Thêm mới một địa chỉ cho cửa hàng hiện tại
     */
    ResponseEntity<BaseResponse> addStoreAddress(StoreAddressRequest req);

    /**
     * ✏️ Cập nhật địa chỉ theo addressId
     */
    ResponseEntity<BaseResponse> updateStoreAddress(UUID addressId, StoreAddressRequest req);

    /**
     * ❌ Xóa một địa chỉ theo addressId
     */
    ResponseEntity<BaseResponse> deleteStoreAddress(UUID addressId);

    /**
     * 🌟 Đặt một địa chỉ làm mặc định theo addressId
     */
    ResponseEntity<BaseResponse> setDefaultAddress(UUID addressId);

    /**
     * 🔍 Tìm kiếm store
     */
    ResponseEntity<BaseResponse> searchStores(String keyword, int page, int size);

    ResponseEntity<BaseResponse<?>> getDefaultAddressByProductId(UUID productId);

    ResponseEntity<BaseResponse> shopToggleStoreStatus(UUID storeId, StoreStatus newStatus);






}
