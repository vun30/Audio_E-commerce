package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellBannerImageResponse {
    private UUID id;
    private String imageUrl;
    private String redirectUrl;
    private String altText;
    private Integer sortOrder;
}
