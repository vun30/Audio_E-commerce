package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FlatGhnShipFeeOverviewResponse {

    private LocalDateTime from;
    private LocalDateTime toExclusive;

    // store_order part
    private BigDecimal orderShipDelivered;      // SUM shipReal (delivered)
    private BigDecimal orderShipReturning15;    // SUM shipReal*1.5 (returnChargeApplied)

    // return_shipping_fees part
    private BigDecimal returnShipFee;           // SUM return_shipping_fees.shipping_fee

    // total
    private BigDecimal totalGhnShipFee;

    private String note;
}
