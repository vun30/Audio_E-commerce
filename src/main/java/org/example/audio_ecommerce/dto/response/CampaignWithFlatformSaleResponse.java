package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.SlotStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignWithFlatformSaleResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private CampaignType type;
    private VoucherStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowRegistration;
    private String badgeLabel;
    private String badgeColor;
    private String badgeIconUrl;

    // Nếu là FAST_SALE → có thêm flatformsale
    private FlatformSaleDetail flatformsale;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlatformSaleDetail {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private CampaignType campaignType;
        private String badgeLabel;
        private String badgeColor;
        private String badgeIconUrl;
        private VoucherStatus status;
        private Boolean allowRegistration;
        private String approvalRule;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
        private UUID createdBy;
        private List<FlashSlotDto> slots;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlashSlotDto {
        private UUID id;
        private LocalDateTime openTime;
        private LocalDateTime closeTime;
        private SlotStatus status;
    }
}
