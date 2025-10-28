package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformCampaignStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformCampaignStoreRepository extends JpaRepository<PlatformCampaignStore, UUID> {
    boolean existsByCampaign_IdAndStore_StoreId(UUID campaignId, UUID storeId);
    List<PlatformCampaignStore> findAllByStore_StoreIdAndApprovedTrue(UUID storeId);
    Optional<PlatformCampaignStore> findByCampaign_IdAndStore_StoreId(UUID campaignId, UUID storeId);
}
