package org.example.audio_ecommerce.dto.response;
import lombok.*;
import org.example.audio_ecommerce.entity.PlatformCampaignProduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformCampaignProductResponse {

    private UUID productId;
    private String productName;
    private Integer discountPercent;
    private Boolean approved;

    public static PlatformCampaignProductResponse fromEntity(PlatformCampaignProduct entity) {
        return PlatformCampaignProductResponse.builder()
                .productId(entity.getProduct().getProductId())
                .productName(entity.getProduct().getName())
                .discountPercent(entity.getDiscountPercent())
                .approved(entity.getApproved())
                .build();
    }
}
