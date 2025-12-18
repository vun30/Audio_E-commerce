package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StorePayoutItemResponse {
    private UUID itemId;
    private UUID storeOrderId;
    private String orderCode;

    private BigDecimal finalLineTotal;
    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount;
    private BigDecimal netAfterFee;

    private Boolean eligibleForPayout;
    private Boolean isPayout;
    private Boolean isReturned;
    private LocalDateTime deliveredAt;
}
