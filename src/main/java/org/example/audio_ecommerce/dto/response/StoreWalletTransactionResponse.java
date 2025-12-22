// dto/response/StoreWalletTransactionResponse.java
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionStatus;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreWalletTransactionResponse {
    private UUID transactionId;
    private UUID walletId;
    private UUID orderId;
    private BigDecimal amount;

    private BigDecimal balanceBefore;          // ✅ thêm
    private BigDecimal balanceAfter;

    private String description;
    private LocalDateTime createdAt;

    private StoreWalletTransactionType type;
    private String displayType;

    private StoreWalletTransactionStatus status; // ✅ thêm
    private String externalRef;
}
