package org.example.audio_ecommerce.dto.response;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatGhnOverviewResponse {

    private LocalDateTime from;
    private LocalDateTime toExclusive;

    // Flat nợ GHN (ship)
    private BigDecimal flatDebtShipToGHN;

    // Cus trả ship (delivered)
    private BigDecimal customerShipPaid;

    // Store nợ Flat (nguồn: wallet debt_balance)
    private BigDecimal storeDebtOutstandingToFlat;

    // Store đã trả Flat (nguồn: store_order.paid_by_shop = true)
    private BigDecimal storeDebtPaidToFlat;

    private BigDecimal storeDebtTotalToFlat;

    private String note;
}