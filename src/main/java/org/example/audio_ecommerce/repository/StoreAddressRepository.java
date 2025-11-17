package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;

public interface StoreAddressRepository extends JpaRepository<StoreAddressEntity, UUID> {

    List<StoreAddressEntity> findByStore_StoreId(UUID storeId);

    void deleteByStore_StoreId(UUID storeId);

    @Modifying
    @Query("UPDATE StoreAddressEntity a SET a.defaultAddress = false WHERE a.store.storeId = ?1")
    void removeDefaultFromAll(UUID storeId);
}
