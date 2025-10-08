package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    // ✅ Sửa lại: dùng field "id" của Account kế thừa từ BaseEntity
    Optional<Store> findByAccount_Id(UUID accountId);

    boolean existsByAccount_Id(UUID accountId);

    // 🔍 Tìm store theo email tài khoản (hay dùng cho login dashboard store)
    Optional<Store> findByAccount_Email(String email);

    // 🔍 Lấy danh sách tất cả store theo trạng thái (INACTIVE, PENDING, ACTIVE, ...)
    List<Store> findAllByStatus(StoreStatus status);

    // 🔍 Kiểm tra xem store đã tồn tại theo tên (để tránh tạo trùng tên)
    boolean existsByStoreName(String storeName);

    // 🔍 Lấy store theo ID nhưng load luôn account (nếu cần)
    Optional<Store> findByStoreId(UUID storeId);

     Page<Store> findByStoreNameContainingIgnoreCase(String keyword, Pageable pageable);

    // ✅ (Tuỳ chọn) tìm theo ID và trạng thái — dùng nếu muốn check nhanh
    Optional<Store> findByStoreIdAndStatus(UUID storeId, StoreStatus status);


}
