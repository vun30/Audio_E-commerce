// org.example.audio_ecommerce.repository.PlatformCampaignProductRepository
package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformCampaignProduct;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformCampaignProductRepository extends JpaRepository<PlatformCampaignProduct, UUID> {

    boolean existsByCampaign_IdAndProduct_ProductId(UUID campaignId, UUID productId);

    List<PlatformCampaignProduct> findAllByCampaign_Id(UUID campaignId);

    List<PlatformCampaignProduct> findAllByCampaign_IdAndFlashSlot_Id(UUID campaignId, UUID slotId);

    @Query("select p from PlatformCampaignProduct p " +
           "where (:campaignId is null or p.campaign.id = :campaignId) " +
           "and (:slotId is null or p.flashSlot.id = :slotId) " +
           "and (:status is null or p.status = :status) " +
           "and (:fromTime is null or p.startTime >= :fromTime) " +
           "and (:toTime is null or p.endTime <= :toTime)")
    List<PlatformCampaignProduct> filter(UUID campaignId, UUID slotId, VoucherStatus status,
                                         LocalDateTime fromTime, LocalDateTime toTime);

    @Modifying
    @Query("update PlatformCampaignProduct p set p.status = :status " +
           "where p.flashSlot.id in :slotIds")
    int bulkUpdateStatusBySlot(List<UUID> slotIds, VoucherStatus status);
}
