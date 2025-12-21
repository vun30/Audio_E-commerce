package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatGhnOverviewAdminResponse {

    private LocalDateTime  from;
    private LocalDateTime toExclusive;

    // 1) Flat nợ GHN (order shipping debt)
    private BigDecimal flatDebtShipToGHN;

    // 2) Khách đã trả ship (delivered)
    private BigDecimal customerShipPaid;

    // 3) Store nợ Flat (nguồn: wallet)
    private BigDecimal storeDebtOutstandingToFlat; // SUM store_wallets.debt_balance
    private BigDecimal storeDebtPaidToFlat;        // SUM transactions trả nợ
    private BigDecimal storeDebtTotalToFlat;       // paid + outstanding

    private String note;
}