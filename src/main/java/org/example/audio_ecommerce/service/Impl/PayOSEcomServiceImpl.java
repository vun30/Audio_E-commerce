// service/Impl/PayOSEcomServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.CustomerOrderItem;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSEcomServiceImpl implements PayOSEcomService {

    private final PayOS payOS;
    private final CustomerOrderRepository customerOrderRepository;
    private final SettlementService settlementService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PlatformTransactionRepository platformTransactionRepository;

    private long generateOrderCode() {
        return System.currentTimeMillis() + new Random().nextInt(999);
    }

    @Override
    @Transactional
    public CheckoutOnlineResponse createPaymentForCustomerOrder(UUID customerOrderId,
                                                                BigDecimal amount,
                                                                String description,
                                                                String returnUrl,
                                                                String cancelUrl) {
        CustomerOrder order = customerOrderRepository.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        long orderCode = generateOrderCode();

        try {
            String descRaw = "Order " + customerOrderId.toString().substring(0, 8);
            String desc = cutUtf8Bytes(asciiNoMarks(descRaw), 20);
            String itemName = cutUtf8Bytes(asciiNoMarks("Order#" + customerOrderId.toString().substring(0, 8)), 20);

            ItemData item = ItemData.builder()
                    .name(itemName)
                    .price(amount.intValueExact())
                    .quantity(1)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(amount.intValueExact())
                    .description(desc)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData res = payOS.createPaymentLink(paymentData);

            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setExternalOrderCode(String.valueOf(orderCode));
            customerOrderRepository.save(order);

            CheckoutOnlineResponse out = new CheckoutOnlineResponse();
            out.setCustomerOrderId(order.getId());
            out.setAmount(amount);
            out.setPayOSOrderCode(orderCode);
            out.setCheckoutUrl(res.getCheckoutUrl());
            out.setQrCode(res.getQrCode());
            out.setStatus(OrderStatus.PENDING.name());
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Tạo link PayOS thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void confirmWebhook(Webhook webhook) {
        try {
            log.info("[PayOS Webhook] raw={}", mapper.writeValueAsString(webhook));
        } catch (Exception ignore) {}

        WebhookData data = webhook.getData();
        if (data == null || data.getOrderCode() == null) {
            log.error("[PayOS Webhook] data/orderCode null. webhook={}", webhook);
            return;
        }
        Long code = data.getOrderCode();
        String resultCode = webhook.getCode();
        Boolean success = webhook.getSuccess();
        String desc = webhook.getDesc();

        CustomerOrder order = customerOrderRepository.findByExternalOrderCode(String.valueOf(code)).orElse(null);
        if (order == null) {
            log.error("[PayOS Webhook] order not found by externalOrderCode={}", code);
            return;
        }

        log.info("[PayOS Webhook] orderId={} code={} resultCode={} success={} desc={}",
                order.getId(), code, resultCode, success, desc);

        // Hết hạn
        if (desc != null && desc.toLowerCase().contains("hết hạn")) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCreatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);
            log.warn("[PayOS Webhook] Order expired. Mark CANCELLED orderId={}", order.getId());
            return;
        }

        if (Boolean.TRUE.equals(success) && "00".equals(resultCode)) {
            // Tính amount an toàn int VND
            BigDecimal amount = order.getItems().stream()
                    .map(i -> i.getLineTotal().setScale(0, java.math.RoundingMode.DOWN))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int amountVnd;
            try {
                amountVnd = amount.intValueExact();
            } catch (ArithmeticException ex) {
                log.error("Amount invalid (must be integer VND <= Integer.MAX_VALUE): {}", amount, ex);
                throw ex;
            }

            // 1) WalletTransaction QR (idempotency)
            // (tùy bạn: check tồn tại theo (orderId, QR))
            settlementService.recordCustomerQrPayment(order.getCustomer().getId(), order.getId(), amount);

            // 2) Platform hold (idempotency)
            boolean existsHolding = !platformTransactionRepository
                    .findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING)
                    .isEmpty();
            if (!existsHolding) {
                settlementService.moveToPlatformHold(order.getId(), amount);
            } else {
                log.info("Platform HOLDING already exists for orderId={} -> skip create", order.getId());
            }

            // 3) Allocate pending to stores
            settlementService.allocateToStoresPending(order);

            // 4) Update order
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);

            log.info("[PayOS Webhook] SUCCESS processed orderId={} amountVnd={}", order.getId(), amountVnd);
            return;
        }

        // thất bại/hủy
        order.setStatus(OrderStatus.UNPAID);
        order.setCreatedAt(LocalDateTime.now());
        customerOrderRepository.save(order);
        log.warn("[PayOS Webhook] FAILED/HUMAN CANCEL orderId={}", order.getId());
    }


    private static String asciiNoMarks(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "");
    }

    private static String cutUtf8Bytes(String s, int maxBytes) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (b.length <= maxBytes) return s;
        int end = maxBytes;
        while (end > 0 && (b[end] & 0xC0) == 0x80) end--;
        return new String(b, 0, end, java.nio.charset.StandardCharsets.UTF_8);
    }
}
