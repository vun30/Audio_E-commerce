package org.example.audio_ecommerce.repository.projection;

import java.math.BigDecimal;

public interface StoreDebtAgg {
    BigDecimal getStoreDebtOutstandingToFlat();
    BigDecimal getStoreDebtPaidToFlat();
}
