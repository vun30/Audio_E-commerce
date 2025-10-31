package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignRequest {

    private String name;
    private String description;
    private String badgeLabel;
    private String badgeColor;
    private String badgeIconUrl;
    private Boolean allowRegistration;
    private String approvalRule;
    private String status; // ACTIVE / DRAFT / DISABLED / EXPIRED / APPROVE
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private List<FlashSlotUpdateDto> flashSlots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlashSlotUpdateDto {
        private UUID id; // null = slot mới, có id = update slot cũ
        private LocalDateTime openTime;
        private LocalDateTime closeTime;
        private String status; // PENDING / ACTIVE / CLOSED / DISABLED
    }
}
