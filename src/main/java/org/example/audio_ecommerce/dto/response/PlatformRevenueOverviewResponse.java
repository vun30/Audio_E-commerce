package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlatformRevenueOverviewResponse {

    private Long deliveredItemCount;      // số item đã giao
    private BigDecimal totalItemRevenue;   // tổng finalLineTotal
    private BigDecimal platformFeeRevenue; // tổng phí nền tảng đã thu
}