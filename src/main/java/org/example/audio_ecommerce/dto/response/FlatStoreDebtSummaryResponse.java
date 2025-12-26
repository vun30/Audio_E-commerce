package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FlatStoreDebtSummaryResponse {

    private LocalDateTime from;
    private LocalDateTime toExclusive;

    // Flat nợ GHN (tổng nghĩa vụ với GHN)
    private BigDecimal flatDebtToGHN;

    // Store nợ Flat
    private BigDecimal storeDebtOutstandingToFlat; // chưa trả
    private BigDecimal storeDebtPaidToFlat;        // đã trả
    private BigDecimal storeDebtTotalToFlat;       // tổng = outstanding + paid

    private String note;
}
