package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WithdrawResult {
    private UUID storeId;
    private BigDecimal withdrawAmount;
    private BigDecimal balanceAfter;
    private LocalDateTime withdrawAt;
    private UUID transactionId;
}
