package org.example.audio_ecommerce.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface LegalPointService {
    void minusForStore(UUID storeId, int point);
    void minusForCustomer(UUID customerId, int point, String reason);
    void resetCustomerLegalPointAfter30Days();
}


