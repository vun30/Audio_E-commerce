package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformCampaignProductUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformCampaignProductUsageRepository extends JpaRepository<PlatformCampaignProductUsage, UUID> {
    Optional<PlatformCampaignProductUsage> findByCampaignProduct_IdAndCustomer_Id(UUID campaignProductId, UUID customerId);
}
