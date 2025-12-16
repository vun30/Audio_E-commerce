package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StoreTopupCreateRequest {

    @NotNull
    @DecimalMin(value = "1000.00", message = "amount must be >= 1000")
    private BigDecimal amount;

    @NotNull
    private String returnUrl;

    @NotNull
    private String cancelUrl;

    private String description;
}
