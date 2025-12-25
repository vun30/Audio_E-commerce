package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPayoutRevenueStatsResponse {

    private Integer year;
    private Integer month;

    // filter storeIds
    private List<UUID> storeIds;

    // range
    private String from; // ISO string for debug
    private String to;

    // metrics
    private Long eligibleItemCount;       // số item eligible
    private Long eligibleOrderCount;      // số đơn có item eligible

    private BigDecimal eligibleGross;         // sum(final_line_total)
    private BigDecimal platformFeeCollected;  // sum(platform_fee_amount)

    private BigDecimal avgPlatformFeePerItem; // platformFeeCollected / eligibleItemCount
}
