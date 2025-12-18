package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreRiskWarningLevel;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRiskWarningResponse {
    private UUID storeId;
    private String storeName;
    private StoreStatus storeStatus;

    private StoreRiskWarningLevel warningLevel;

    private BigDecimal legalPoint;
    private BigDecimal creditLimit;       // legalPoint * 1,000,000

    private BigDecimal debtBalance;
    private BigDecimal payableNowDebt; // tổng nợ có thể thanh toán ngay
    private BigDecimal depositBalance;
    private BigDecimal effectiveDebt;     // max(0, debt - deposit)

    private BigDecimal warningLine;       // 80% of creditLimit
    private BigDecimal criticalLine;      // 90% of creditLimit

    private LocalDateTime evaluatedAt;
}
