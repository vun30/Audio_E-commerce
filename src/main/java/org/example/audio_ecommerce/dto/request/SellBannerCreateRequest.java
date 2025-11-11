package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellBannerCreateRequest {
    private String title;                 // required
    private String description;           // optional
    private String bannerType;            // HOME, CAMPAIGN...
    private Boolean active;               // default true
    private LocalDateTime startTime;      // optional
    private LocalDateTime endTime;        // optional
    private List<SellBannerImageRequest> images; // list ảnh + link
}
