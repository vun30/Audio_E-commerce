package org.example.audio_ecommerce.service.Projection;

import java.math.BigDecimal;

public interface FlatShipAgg {
    BigDecimal getOrderShipDelivered();
    BigDecimal getOrderShipReturning15();
}
