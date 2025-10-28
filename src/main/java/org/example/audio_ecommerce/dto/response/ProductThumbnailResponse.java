package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class ProductThumbnailResponse {

    // ===== PRODUCT =====
    private UUID productId;
    private String name;
    private String brandName;
    private BigDecimal price;
    private String warehouseLocation;
    private String categoryName;
    private String storeName;
    private List<String> images;

    // ===== CAMPAIGN INFO =====
    private String campaignCode;
    private String campaignName;
    private String campaignDescription;

    // ===== BADGE =====
    private String badgeLabel;
    private String badgeColor;
    private String badgeIconUrl;

    // ⚙️ Constructor chính xác mà Hibernate cần
    public ProductThumbnailResponse(
            UUID productId,
            String name,
            String brandName,
            BigDecimal price,
            String warehouseLocation,
            String categoryName,
            String storeName,
            List<String> images,
            String campaignCode,
            String campaignName,
            String campaignDescription,
            String badgeLabel,
            String badgeColor,
            String badgeIconUrl
    ) {
        this.productId = productId;
        this.name = name;
        this.brandName = brandName;
        this.price = price;
        this.warehouseLocation = warehouseLocation;
        this.categoryName = categoryName;
        this.storeName = storeName;
        this.images = images;
        this.campaignCode = campaignCode;
        this.campaignName = campaignName;
        this.campaignDescription = campaignDescription;
        this.badgeLabel = badgeLabel;
        this.badgeColor = badgeColor;
        this.badgeIconUrl = badgeIconUrl;
    }
}
