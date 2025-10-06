package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WalletTxnRequest {
    @NotNull @DecimalMin(value = "0.01") private BigDecimal amount;
    private String description;
    private UUID orderId; // bắt buộc cho PAYMENT/REFUND, có thể null cho DEPOSIT/WITHDRAW
}
