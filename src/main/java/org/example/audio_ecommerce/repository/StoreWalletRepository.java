package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreWalletRepository extends JpaRepository<StoreWallet, UUID> {

    // 🔍 Tìm ví theo ID cửa hàng
    Optional<StoreWallet> findByStore_StoreId(UUID storeId);

    // 🔍 Kiểm tra xem ví đã tồn tại cho store hay chưa
    boolean existsByStore_StoreId(UUID storeId);

    @Query("""
select coalesce(sum(w.debtBalance), 0)
from StoreWallet w
""")
    BigDecimal sumAllStoreDebtBalance();

}
