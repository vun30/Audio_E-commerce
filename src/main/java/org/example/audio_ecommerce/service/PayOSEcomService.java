// service/PayOSEcomService.java
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import vn.payos.type.Webhook;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayOSEcomService {
    CheckoutOnlineResponse createPaymentForCustomerOrder(UUID customerOrderId,
                                                         BigDecimal amount,
                                                         String description,
                                                         String returnUrl,
                                                         String cancelUrl);

    void confirmWebhook(Webhook webhook);
}
