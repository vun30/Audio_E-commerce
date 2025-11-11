package org.example.audio_ecommerce.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellBannerImageRequest {
    private String imageUrl;     // bắt buộc
    private String redirectUrl;  // optional
    private String altText;      // optional
    private Integer sortOrder;   // default 0
}
