package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodEligibilityResponse {
    private boolean overallEligible; // true nếu tất cả store đều đủ điều kiện
    private List<PerStore> stores;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PerStore {
        private UUID storeId;
        private String storeName;

        private BigDecimal storeSubtotal;   // tổng tiền các item thuộc store trong checkout
        private BigDecimal requiredDeposit; // storeSubtotal * ratio (rounding down)
        private BigDecimal depositBalance;  // từ StoreWallet.depositBalance

        private boolean eligible;           // depositBalance >= requiredDeposit
        private String reason;              // ví dụ: "INSUFFICIENT_DEPOSIT"
    }
}
