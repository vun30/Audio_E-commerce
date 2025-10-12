package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformWalletResponse {
    private UUID id;
    private WalletOwnerType ownerType;
    private UUID ownerId;
    private BigDecimal totalBalance;
    private BigDecimal pendingBalance;
    private BigDecimal doneBalance;
    private BigDecimal receivedTotal;
    private BigDecimal refundedTotal;
    private String currency;
    private LocalDateTime createdAt;

    // Tùy chọn — chỉ hiển thị nếu muốn xem chi tiết ví
    private List<PlatformTransactionResponse> transactions;
}
