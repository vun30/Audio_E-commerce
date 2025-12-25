package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnShipFeeSummaryResponse {

    private LocalDateTime from;
    private LocalDateTime toExclusive;

    private BigDecimal totalReturnShipFee;        // tổng ship return
    private BigDecimal returnShipFeePaid;         // store đã trả
    private BigDecimal returnShipFeeOutstanding;  // store chưa trả

    private String note;
}
