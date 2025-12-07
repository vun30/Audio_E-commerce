package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PreviewCampaignPriceResponse {

    private UUID productId;
    private UUID variantId;

    private int quantity;

    // Giá base (giá gốc sau bulk nếu có)
    private BigDecimal baseUnitPrice;

    // Giá unit nếu được campaign áp dụng (nếu không có campaign thì = null)
    private BigDecimal campaignUnitPrice;

    // Giá unit thực tế mà BE sẽ áp nếu add vào cart / checkout
    private BigDecimal effectiveUnitPrice;

    private BigDecimal lineTotal; // effectiveUnitPrice * quantity

    private boolean inCampaign;          // có campaign active không
    private boolean campaignUsageExceeded; // true nếu vượt usage_per_user
    private Integer campaignRemaining;     // số lượng còn được hưởng (null nếu không giới hạn)

    // Optional: để FE hiển thị thêm
    private String campaignName;
    private String campaignCode;
}
