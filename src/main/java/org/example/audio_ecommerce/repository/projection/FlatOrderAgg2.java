package org.example.audio_ecommerce.repository.projection;

import java.math.BigDecimal;

public interface FlatOrderAgg2 {
    BigDecimal getFlatDebtShipToGhn();
    BigDecimal getCustomerShipPaid();
    BigDecimal getStoreDebtPaidToFlat();
}