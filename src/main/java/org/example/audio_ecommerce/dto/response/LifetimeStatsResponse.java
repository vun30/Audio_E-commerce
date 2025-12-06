package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifetimeStatsResponse {

    private long totalDeliveredOrders;

    private BigDecimal totalRevenue;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalNetRevenue;

    private long totalReturnRequests;
    private double returnRate;

    private List<TopProductLifetime> top10Products;
    private TopReturnProduct topReturnProduct;

    private BigDecimal totalShippingDifferenceFee; // phí ship chênh lệch
    private BigDecimal totalReturnShippingFee;      // phí ship return
}
