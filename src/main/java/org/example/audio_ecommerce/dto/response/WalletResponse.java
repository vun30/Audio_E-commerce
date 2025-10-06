package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private UUID customerId;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime lastTransactionAt;
}
