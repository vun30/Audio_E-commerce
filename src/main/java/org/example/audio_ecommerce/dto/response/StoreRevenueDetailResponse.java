package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StoreRevenueDetailResponse {
    private UUID storeOrderId;
    private String orderCode;
    private BigDecimal grandTotal;
    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount;
    private LocalDateTime deliveredAt;
    private String status;
}