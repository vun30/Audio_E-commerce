package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReturnPackageFeeResponse {
    private BigDecimal shippingFee;
}
