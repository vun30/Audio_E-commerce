// service/PayOSEcomService.java
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.dto.response.WalletTopupResponse;
import vn.payos.model.webhooks.WebhookData;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PayOSEcomService {
    CheckoutOnlineResponse createPaymentForMultipleCustomerOrders(
            List<UUID> orderIds,
            BigDecimal totalAmount,
            String description,
            String returnUrl,
            String cancelUrl,
            long batchOrderCode // dùng làm orderCode PayOS
    );
    WalletTopupResponse createWalletTopupPayment(
            UUID customerId,
            BigDecimal amount,
            String description,
            String returnUrl,
            String cancelUrl
    );
    void confirmWebhook(WebhookData webhookData);
}
