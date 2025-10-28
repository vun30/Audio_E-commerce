// org.example.audio_ecommerce.repository.PlatformCampaignFlashSlotRepository
package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.SlotStatus;
import org.example.audio_ecommerce.entity.PlatformCampaignFlashSlot;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformCampaignFlashSlotRepository extends JpaRepository<PlatformCampaignFlashSlot, UUID> {

    List<PlatformCampaignFlashSlot> findAllByCampaign_Id(UUID campaignId);

    List<PlatformCampaignFlashSlot> findAllByStatus(SlotStatus status);

    @Query("select s from PlatformCampaignFlashSlot s " +
           "where (:campaignId is null or s.campaign.id = :campaignId) " +
           "and (:status is null or s.status = :status) " +
           "and (:fromTime is null or s.openTime >= :fromTime) " +
           "and (:toTime is null or s.closeTime <= :toTime)")
    List<PlatformCampaignFlashSlot> filter(UUID campaignId, SlotStatus status,
                                           LocalDateTime fromTime, LocalDateTime toTime);

    @Modifying
    @Query("update PlatformCampaignFlashSlot s set s.status = :status where s.id in :ids")
    int bulkUpdateStatus(List<UUID> ids, SlotStatus status);
}
