package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTransactionResponse {
    private UUID id;
    private UUID walletId;
    private UUID orderId;
    private UUID storeId;
    private UUID customerId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
