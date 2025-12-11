package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContribution {
    private UUID itemId;
    private UUID storeOrderId;
    private String productName;
    private int quantity;
    private BigDecimal lineTotal;
    private BigDecimal shippingFeeEstimated;
    private BigDecimal shippingFeeActual;
    private BigDecimal shippingExtraForStore;
    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount; // computed per item
    private BigDecimal netPayoutItem;     // computed per item after fee
}
