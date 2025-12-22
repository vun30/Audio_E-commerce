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
    private LocalDateTime withdrawAt;
    private UUID transactionId; // storeWalletTxId

    // ✅ trả về rõ ràng trước/sau rút cho shop
    private BigDecimal storeBalanceBefore;
    private BigDecimal storeBalanceAfter;

    // ✅ nếu UI admin/platform cần thì có luôn cash trước/sau
    private BigDecimal platformCashBefore;
    private BigDecimal platformCashAfter;
}