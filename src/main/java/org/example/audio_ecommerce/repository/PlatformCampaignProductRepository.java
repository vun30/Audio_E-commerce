// org.example.audio_ecommerce.repository.PlatformCampaignProductRepository
package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.PlatformCampaignProduct;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    @Query("""
        SELECT pcp
        FROM PlatformCampaignProduct pcp
        WHERE pcp.product.productId = :productId
          AND pcp.status = 'ACTIVE'
          AND :now BETWEEN pcp.startTime AND pcp.endTime
    """)
    Optional<PlatformCampaignProduct> findActiveCampaignVoucherByProduct(
            @Param("productId") UUID productId,
            @Param("now") LocalDateTime now);

    @Query("""
SELECT p
FROM PlatformCampaignProduct p
WHERE p.campaign.id = :campaignId
  AND (:storeId IS NULL OR p.store.storeId = :storeId)
  AND (:status IS NULL OR p.status = :status)
  AND (:from IS NULL OR p.createdAt >= :from)
  AND (:to IS NULL OR p.createdAt <= :to)
ORDER BY p.createdAt DESC
""")
List<PlatformCampaignProduct> filterProducts(
        UUID campaignId,
        UUID storeId,
        VoucherStatus status,
        LocalDateTime from,
        LocalDateTime to
);

List<PlatformCampaignProduct> findAllByCampaign_IdAndProduct_ProductIdIn(UUID campaignId, List<UUID> productIds);

// Nếu chưa có, thêm:
@Query("SELECT p FROM PlatformCampaignProduct p " +
       "WHERE p.campaign.id = :campaignId AND p.product.productId IN :productIds")
List<PlatformCampaignProduct> findByCampaignAndProducts(@Param("campaignId") UUID campaignId,
                                                        @Param("productIds") List<UUID> productIds);



@Query("""
    SELECT p FROM PlatformCampaignProduct p
    WHERE (:campaignType IS NULL OR p.campaign.campaignType = :campaignType)
      AND (:status IS NULL OR p.status = :status)
      AND (:storeId IS NULL OR p.store.storeId = :storeId)
      AND (:campaignId IS NULL OR p.campaign.id = :campaignId)
""")
List<PlatformCampaignProduct> filterCampaignProducts(
        @Param("campaignType") CampaignType campaignType,
        @Param("status") VoucherStatus status,
        @Param("storeId") UUID storeId,
        @Param("campaignId") UUID campaignId  // ✅ thêm filter campaignId
);



}
