package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreWalletRepository extends JpaRepository<StoreWallet, UUID> {

    // 🔍 Tìm ví theo ID cửa hàng
    Optional<StoreWallet> findByStore_StoreId(UUID storeId);

    // 🔍 Kiểm tra xem ví đã tồn tại cho store hay chưa
    boolean existsByStore_StoreId(UUID storeId);
}
