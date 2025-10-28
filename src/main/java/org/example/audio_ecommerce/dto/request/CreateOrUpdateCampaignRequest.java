// org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest
package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateOrUpdateCampaignRequest {
    private String code;                  // "MEGA_SALE_12_12" hoặc "FAST_SALE_11_11"
    private String name;
    private String description;
    private CampaignType campaignType;    // MEGA_SALE | FAST_SALE
    private String badgeLabel;
    private String badgeColor;
    private String badgeIconUrl;
    private Boolean allowRegistration;    // mặc định TRUE
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ✅ Nếu FAST_SALE → truyền danh sách slot
    private List<FlashSlotRequest> flashSlots;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FlashSlotRequest {
        private LocalDateTime openTime;
        private LocalDateTime closeTime;
    }
}
