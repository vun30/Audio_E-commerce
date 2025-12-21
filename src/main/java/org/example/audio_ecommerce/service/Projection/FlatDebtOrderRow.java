package org.example.audio_ecommerce.service.Projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface FlatDebtOrderRow {
    String getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getDeliveredAt();
    BigDecimal getShippingFeeReal();
    BigDecimal getShippingFee();
    Boolean getPaidByShop();
    BigDecimal getGhnPaidByFlat();
}