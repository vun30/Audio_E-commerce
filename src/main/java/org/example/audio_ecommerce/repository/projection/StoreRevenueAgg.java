package org.example.audio_ecommerce.repository.projection;

import java.math.BigDecimal;

public interface StoreRevenueAgg {
    BigDecimal getTotalAmount();
    BigDecimal getTotalPlatformFee();
    BigDecimal getTotalShippingFee();
}
