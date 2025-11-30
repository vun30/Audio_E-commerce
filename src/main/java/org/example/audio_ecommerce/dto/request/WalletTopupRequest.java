package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTopupRequest {
    private BigDecimal amount;
    private String returnUrl;
    private String cancelUrl;
}
