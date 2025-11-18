package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRevenueSummaryResponse {

    private UUID storeId;
    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal totalAmount;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalShippingFee;
}
