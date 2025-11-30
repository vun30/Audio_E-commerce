package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WalletTopupResponse {
    private UUID walletTransactionId;
    private BigDecimal amount;
    private Long payOSOrderCode;
    private String checkoutUrl;
    private String status; // PENDING / ...
}
