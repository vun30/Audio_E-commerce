package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    // Tìm store theo accountId (mỗi account có thể có 0 hoặc 1 store)
    Optional<Store> findByAccount_AccountId(UUID accountId);

    // Kiểm tra account đã có store chưa
    boolean existsByAccount_AccountId(UUID accountId);
}
