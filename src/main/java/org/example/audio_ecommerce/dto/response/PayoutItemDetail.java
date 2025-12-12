package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutItemDetail {

    private UUID itemId;
    private String productName;
    private int quantity;

    private BigDecimal finalLineTotal;
    private BigDecimal shippingFeeEstimated;
    private BigDecimal shippingFeeActual;
    private BigDecimal returnShippingFee;

    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount;

    private BigDecimal netAmount;
    private LocalDateTime deliveredAt;
}
