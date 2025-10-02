package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    // Tìm store theo id của account
    Optional<Store> findByAccount_Id(UUID accountId);

    // Kiểm tra account đã có store chưa
    boolean existsByAccount_Id(UUID accountId);
}
