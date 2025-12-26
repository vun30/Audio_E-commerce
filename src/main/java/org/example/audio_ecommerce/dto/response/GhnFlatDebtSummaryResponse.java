package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnFlatDebtSummaryResponse {

    private String scope; // ALL_SYSTEM | STORE_ONLY
    private UUID storeId; // null nếu ALL_SYSTEM

    private BigDecimal flatDebtToGHN;

    private BigDecimal customerPaidTotal;

    private DebtAmountBreakdownDto storeOrderDebtToFlat;

    private DebtAmountBreakdownDto returnFeeDebtToFlat;

    private String formula;
}
