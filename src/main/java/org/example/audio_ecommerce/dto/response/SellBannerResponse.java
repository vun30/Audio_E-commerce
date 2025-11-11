package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellBannerResponse {
    private UUID id;
    private String title;
    private String description;
    private String bannerType;
    private Boolean active;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private List<SellBannerImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
