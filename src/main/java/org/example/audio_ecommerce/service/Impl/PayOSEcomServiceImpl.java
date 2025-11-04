// service/Impl/PayOSEcomServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.email.OrderData;
import org.example.audio_ecommerce.email.OrderItemEmailData;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.CustomerOrderItem;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSEcomServiceImpl implements PayOSEcomService {
    private static final DateTimeFormatter PAID_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final PayOS payOS;
    private final CustomerOrderRepository customerOrderRepository;
    private final SettlementService settlementService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final EmailService emailService;
    private final PlatformTransactionRepository platformTransactionRepository;

    private long generateOrderCode() {
        return System.currentTimeMillis() + new Random().nextInt(999);
    }

    @Override
    @Transactional
    public CheckoutOnlineResponse createPaymentForMultipleCustomerOrders(
            List<UUID> orderIds,
            BigDecimal totalAmount,
            String description,
            String returnUrl,
            String cancelUrl,
            long batchOrderCode
    ) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("orderIds is empty");
        }
        List<CustomerOrder> orders = customerOrderRepository.findAllById(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new NoSuchElementException("Some CustomerOrder not found");
        }

        BigDecimal payAmount = (totalAmount != null ? totalAmount : orders.stream()
                .map(o -> o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .setScale(0, java.math.RoundingMode.DOWN);
        int amountVnd = payAmount.intValueExact();

        String desc = cutUtf8Bytes(asciiNoMarks(description != null ? description : "Group orders"), 20);
        String itemName = cutUtf8Bytes(asciiNoMarks("Group#" + batchOrderCode), 20);

        try {
            ItemData item = ItemData.builder()
                    .name(itemName)
                    .price(amountVnd)
                    .quantity(1)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(batchOrderCode)  // <- 1 code cho cả nhóm
                    .amount(amountVnd)
                    .description(desc)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData res = payOS.createPaymentLink(paymentData);

            // GẮN batchCode vào JSON của từng order, nhưng KHÔNG đụng externalOrderCode Eᵢ
            for (CustomerOrder order : orders) {
                // Optional: nếu order CHƯA có externalOrderCode riêng thì sinh luôn ở đây:
                if (order.getExternalOrderCode() == null || order.getExternalOrderCode().isBlank()) {
                    long perOrderCode = System.currentTimeMillis() + new java.util.Random().nextInt(999);
                    order.setExternalOrderCode(String.valueOf(perOrderCode)); // Eᵢ riêng
                }

                // Nhúng batch vào platformVoucherDetailJson
                String json = order.getPlatformVoucherDetailJson();
                com.fasterxml.jackson.databind.node.ObjectNode node;
                if (json != null && !json.isBlank()) {
                    node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
                } else {
                    node = mapper.createObjectNode();
                }
                node.put("__payos_batch_code", String.valueOf(batchOrderCode));
                order.setPlatformVoucherDetailJson(mapper.writeValueAsString(node));

                // Đánh dấu chờ thanh toán (tuỳ flow)
                order.setStatus(OrderStatus.PENDING);
                order.setCreatedAt(LocalDateTime.now());
                if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
                    order.setPaymentMethod(PaymentMethod.ONLINE); // đảm bảo gắn ONLINE
                }
                customerOrderRepository.save(order);
            }

            CheckoutOnlineResponse out = new CheckoutOnlineResponse();
            out.setCustomerOrderId(orders.get(0).getId()); // tuỳ bạn, có thể null
            out.setAmount(payAmount);
            out.setPayOSOrderCode(batchOrderCode);
            out.setCheckoutUrl(res.getCheckoutUrl());
            out.setQrCode(res.getQrCode());
            out.setStatus(OrderStatus.PENDING.name());
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Tạo link PayOS (group) thất bại: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public void confirmWebhook(Webhook webhook) {
        try { log.info("[PayOS Webhook] raw={}", mapper.writeValueAsString(webhook)); } catch (Exception ignore) {}

        WebhookData data = webhook.getData();
        if (data == null || data.getOrderCode() == null) {
            log.error("[PayOS Webhook] data/orderCode null. webhook={}", webhook);
            return;
        }
        Long batchCode = data.getOrderCode();
        String resultCode = webhook.getCode();
        Boolean success = webhook.getSuccess();
        String desc = webhook.getDesc();

        // Tìm tất cả order có JSON chứa __payos_batch_code = batchCode
        // Nếu bạn chưa có repo method, dùng findAll rồi filter bằng code dưới (đơn giản, nhưng tốt nhất là thêm method custom query JSON).
        List<CustomerOrder> all = customerOrderRepository.findAll(); // hoặc custom query tốt hơn
        List<CustomerOrder> orders = new ArrayList<>();
        for (CustomerOrder o : all) {
            String json = o.getPlatformVoucherDetailJson();
            if (json == null || json.isBlank()) continue;
            try {
                var node = mapper.readTree(json);
                if (node.has("__payos_batch_code") &&
                        String.valueOf(batchCode).equals(node.get("__payos_batch_code").asText())) {
                    orders.add(o);
                }
            } catch (Exception ignored) {}
        }

        if (orders.isEmpty()) {
            log.error("[PayOS Webhook] no orders found for batch={}", batchCode);
            return;
        }

        log.info("[PayOS Webhook] batch={} size={} resultCode={} success={} desc={}",
                batchCode, orders.size(), resultCode, success, desc);

        if (desc != null && desc.toLowerCase().contains("hết hạn")) {
            for (CustomerOrder order : orders) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCreatedAt(LocalDateTime.now());
                customerOrderRepository.save(order);
            }
            return;
        }

        if (Boolean.TRUE.equals(success) && "00".equals(resultCode)) {
            for (CustomerOrder order : orders) {
                BigDecimal amount = order.getGrandTotal() != null ? order.getGrandTotal() : order.getTotalAmount();
                if (amount == null) amount = BigDecimal.ZERO;
                amount = amount.setScale(0, java.math.RoundingMode.DOWN);

                settlementService.recordCustomerQrPayment(order.getCustomer().getId(), order.getId(), amount);
                boolean existsHolding = !platformTransactionRepository
                        .findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING)
                        .isEmpty();
                if (!existsHolding) {
                    settlementService.moveToPlatformHold(order.getId(), amount);
                }
                settlementService.allocateToStoresPending(order);

                order.setStatus(OrderStatus.PENDING); // hoặc CONFIRMED
                order.setCreatedAt(LocalDateTime.now());
                customerOrderRepository.save(order);

                sendPaymentSuccessEmail(order, amount);
            }
            log.info("[PayOS Webhook] SUCCESS processed batch={} orders={}", batchCode, orders.size());
            return;
        }

        // failed/cancel
        for (CustomerOrder order : orders) {
            order.setStatus(OrderStatus.UNPAID);
            order.setCreatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);
        }
    }



    private void sendPaymentSuccessEmail(CustomerOrder order, BigDecimal amount) {
        if (order.getCustomer() == null || !StringUtils.hasText(order.getCustomer().getEmail())) {
            log.warn("[PayOS Webhook] Skip email - missing customer email for orderId={}", order.getId());
            return;
        }

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormatter.setMaximumFractionDigits(0);
        currencyFormatter.setMinimumFractionDigits(0);
        List<OrderItemEmailData> items = new ArrayList<>();
        for (CustomerOrderItem item : order.getItems()) {
            items.add(OrderItemEmailData.builder()
                    .name(item.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(currencyFormatter.format(item.getUnitPrice()))
                    .lineTotal(currencyFormatter.format(item.getLineTotal()))
                    .build());
        }

        String customerName = order.getCustomer() != null &&
                StringUtils.hasText(order.getCustomer().getFullName())
                ? order.getCustomer().getFullName()
                : "Quý khách";

        String paidAt = PAID_AT_FORMATTER.format(LocalDateTime.now());

        OrderData emailData = OrderData.builder()
                .email(order.getCustomer().getEmail())
                .customerName(customerName)
                .orderCode(order.getExternalOrderCode())
                .totalAmount(currencyFormatter.format(amount))
                .paidAt(paidAt)
                .receiverName(order.getShipReceiverName())
                .shippingAddress(buildShippingAddress(order))
                .phoneNumber(order.getShipPhoneNumber())
                .shippingNote(order.getShipNote())
                .items(items)
                .build();

        try {
            emailService.sendEmail(EmailTemplateType.ORDER_CONFIRMED, emailData);
        } catch (MessagingException e) {
            log.error("[PayOS Webhook] Failed to send payment success email for orderId={}", order.getId(), e);
        }
    }

    private String buildShippingAddress(CustomerOrder order) {
        List<String> parts = new ArrayList<>();
        addIfHasText(parts, order.getShipAddressLine());
        addIfHasText(parts, order.getShipStreet());
        addIfHasText(parts, order.getShipWard());
        addIfHasText(parts, order.getShipDistrict());
        addIfHasText(parts, order.getShipProvince());
        addIfHasText(parts, order.getShipPostalCode());
        addIfHasText(parts, order.getShipCountry());
        return parts.isEmpty() ? "" : String.join(", ", parts);
    }

    private void addIfHasText(List<String> parts, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String sanitized = value.trim();
        if (!parts.contains(sanitized)) {
            parts.add(sanitized);
        }
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
