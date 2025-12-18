package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DepositTransferResult {
    private UUID storeId;
    private BigDecimal amount;

    private BigDecimal defaultBalanceAfter;
    private BigDecimal depositBalanceAfter;

    private UUID transactionId;
    private LocalDateTime transferredAt;
}
