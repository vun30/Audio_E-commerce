package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PayDebtResult {
    private UUID storeId;
    private UUID transactionId;
    private BigDecimal paidAmount;
    private BigDecimal balanceAfter;
    private int paidOrdersCount;
    private int paidReturnFeesCount;
    private LocalDateTime paidAt;
}
