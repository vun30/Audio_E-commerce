package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponse {

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
    private LocalDateTime updatedAt;

    private List<FlashSlotDto> flashSlots;

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
