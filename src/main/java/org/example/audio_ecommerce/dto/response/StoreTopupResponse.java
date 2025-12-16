package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StoreTopupResponse {
    private UUID transactionId;
    private BigDecimal amount;
    private long payOSOrderCode;
    private String checkoutUrl;
    private String status;
}
