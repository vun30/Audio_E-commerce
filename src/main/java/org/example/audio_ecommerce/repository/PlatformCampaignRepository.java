package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.PlatformCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlatformCampaignRepository extends JpaRepository<PlatformCampaign, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
        SELECT c FROM PlatformCampaign c
        WHERE (:type IS NULL OR c.campaignType = :type)
          AND (:status IS NULL OR c.status = :status)
          AND (:start IS NULL OR c.startTime >= :start)
          AND (:end IS NULL OR c.endTime <= :end)
        ORDER BY c.startTime DESC
    """)
    List<PlatformCampaign> filterCampaigns(
            @Param("type") CampaignType type,
            @Param("status") VoucherStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
