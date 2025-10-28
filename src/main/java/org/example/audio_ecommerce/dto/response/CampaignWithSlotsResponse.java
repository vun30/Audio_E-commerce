// org.example.audio_ecommerce.dto.response.CampaignWithSlotsResponse
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.SlotStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignWithSlotsResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private CampaignType campaignType;
    private String badgeLabel;
    private String badgeColor;
    private String badgeIconUrl;
    private Boolean allowRegistration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private VoucherStatus status;

    private List<SlotDto> slots; // null nếu MEGA_SALE

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SlotDto {
        private UUID id;
        private LocalDateTime openTime;
        private LocalDateTime closeTime;
        private SlotStatus status;
    }
}
