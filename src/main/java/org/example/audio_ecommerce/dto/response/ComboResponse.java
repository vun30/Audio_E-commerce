package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ComboResponse {
    private UUID comboId;
    private UUID comboProductId;
    private String comboName;
    private String comboImageUrl;
    private String categoryName;
    private String categoryIconUrl;
    private String comboDescription;
    private BigDecimal comboPrice;
    private BigDecimal originalTotalPrice;
    private Boolean isActive;
    private List<UUID> includedProductIds;
}
