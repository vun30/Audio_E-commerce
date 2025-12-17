package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StorePayoutSummaryResponse {

    // 1) Pending balance (hold)
    private long pendingCount;
    private BigDecimal pendingGross;   // tổng finalLineTotal của item hold

    // 2) Eligible but not payout yet => platform fee payable
    private long eligibleNotPayoutCount;
    private BigDecimal eligibleNotPayoutGross; // tổng finalLineTotal
    private BigDecimal platformFeePayable;     // tổng phí nền tảng phải trả (gross * %)

    // 3) Available balance (already payout)
    private long payoutDoneCount;
    private BigDecimal availableGross;   // tổng finalLineTotal
    private BigDecimal platformFeePaid;  // tổng phí nền tảng đã trừ
    private BigDecimal availableNet;     // gross - fee (tiền khả dụng)
}
