// dto/response/StoreWalletSummaryResponse.java
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreWalletSummaryResponse {
    private UUID storeId;
    private String storeName;
    private UUID walletId;
     private BigDecimal depositBalance;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalRevenue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
