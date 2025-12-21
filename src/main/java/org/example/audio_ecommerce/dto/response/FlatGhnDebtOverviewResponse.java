package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatGhnDebtOverviewResponse {

    // ===== FILTER =====
    private LocalDateTime from;
    private LocalDateTime to;

    // ===== GHN =====
    private BigDecimal flatDebtToGHN;        // tổng nợ GHN
    private BigDecimal flatPaidToGHN;        // ✅ flat đã trả GHN
    private BigDecimal flatOutstandingToGHN; // ✅ flat còn nợ GHN

    // ===== STORE =====
    private BigDecimal storeDebtOutstandingToFlat;
    private BigDecimal storeDebtPaidToFlat;
    private BigDecimal storeDebtTotalToFlat;

    // ===== RETURN =====
    private BigDecimal returnDebtOutstanding;
    private BigDecimal returnDebtPaid;
    private BigDecimal returnDebtTotal;

    // ===== CUSTOMER =====
    private BigDecimal customerShipPaid;

    // ===== RESULT =====
    private BigDecimal flatNet;

    private String note;
}