package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PlatformVoucherUsageResponse {
    private UUID id;

    private UUID campaignId;
    private String campaignCode;
    private String campaignName;
    private String campaignType;

    private UUID campaignProductId;
    private UUID productId;
    private String productName;
    private UUID storeId;
    private String storeName;

    private UUID customerId;
    private String customerName;
    private String customerEmail;

    private Integer usedCount;
    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;
}
