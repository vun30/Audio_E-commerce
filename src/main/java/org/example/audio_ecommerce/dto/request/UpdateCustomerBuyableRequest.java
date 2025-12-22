package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCustomerBuyableRequest {
    @NotNull
    private Boolean buyable;
}
