package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StoreTotalRevenueResponse {
    private UUID storeId;
    private BigDecimal totalPlatformFeeRevenue;
    private Long totalDeliveredOrders;
    private List<StoreRevenueDetailResponse> deliveredOrders;
}