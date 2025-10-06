package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.StoreKyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreKycRepository extends JpaRepository<StoreKyc, String> {
    boolean existsByStore_StoreIdAndStatus(UUID storeId, KycStatus status);

    // 📜 Lấy toàn bộ request của 1 cửa hàng
    List<StoreKyc> findByStore_StoreIdOrderByCreatedAtDesc(UUID storeId);

    // 📜 Lấy toàn bộ request theo trạng thái (dành cho admin dashboard)
    List<StoreKyc> findByStatusOrderBySubmittedAtDesc(KycStatus status);
}