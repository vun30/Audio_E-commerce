package org.example.audio_ecommerce.repository.projection;

import java.math.BigDecimal;

public interface ReturnShipFeeAgg {

    BigDecimal getPaid();
    BigDecimal getOutstanding();
}
