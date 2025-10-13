// dto/response/StoreWalletTransactionResponse.java
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreWalletTransactionResponse {
    /** 🔑 ID giao dịch ví */
    private UUID transactionId;

    /** 🏦 ID ví liên quan */
    private UUID walletId;

    /** 📦 Mã đơn hàng liên quan (nếu có) */
    private UUID orderId;

    /** 💰 Số tiền thay đổi trong giao dịch */
    private BigDecimal amount;

    /** 💸 Số dư sau giao dịch */
    private BigDecimal balanceAfter;

    /** 🧾 Mô tả chi tiết giao dịch */
    private String description;

    /** 📅 Thời gian thực hiện giao dịch */
    private LocalDateTime createdAt;

    /** 🔁 Loại giao dịch (Enum) — DEPOSIT, WITHDRAW, REFUND, ... */
    private StoreWalletTransactionType type;

    /** 🌐 Tên hiển thị thân thiện cho FE (VD: "Nạp tiền", "Rút tiền") */
    private String displayType;
}
