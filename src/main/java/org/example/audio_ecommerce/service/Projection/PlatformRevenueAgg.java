package org.example.audio_ecommerce.service.Projection;

import java.math.BigDecimal;

public interface PlatformRevenueAgg {
    Long getDeliveredItemCount();
    BigDecimal  getTotalItemRevenue();
    BigDecimal getPlatformFeeRevenue();
}
