package org.example.audio_ecommerce.service.Projection;

import java.math.BigDecimal;

public interface FlatOrderAgg {
    BigDecimal getFlatDebtShipToGhn();
    BigDecimal getCustomerShipPaid();
}