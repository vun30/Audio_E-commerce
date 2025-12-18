package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WithdrawDepositToDefaultResult {
    private UUID storeId;
    private BigDecimal amount;

    private BigDecimal depositBefore;
    private BigDecimal depositAfter;

    private BigDecimal defaultBefore;
    private BigDecimal defaultAfter;

    private BigDecimal debtNow;
    private BigDecimal creditAfter;

    private UUID transactionId;
    private LocalDateTime createdAt;
}
