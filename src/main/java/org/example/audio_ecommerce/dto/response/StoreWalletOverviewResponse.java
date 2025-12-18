package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreWalletOverviewResponse {

    private UUID storeId;
    private String storeName;     // nếu Store có
    private UUID walletId;

    private BigDecimal defaultBalance;
    private BigDecimal depositBalance;
    private BigDecimal debtBalance;
}
