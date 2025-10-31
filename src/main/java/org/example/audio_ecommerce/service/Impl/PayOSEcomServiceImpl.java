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
    public CheckoutOnlineResponse createPaymentForCustomerOrder(UUID customerOrderId,
                                                                BigDecimal amount,
                                                                String description,
                                                                String returnUrl,
                                                                String cancelUrl) {
        CustomerOrder order = customerOrderRepository.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        // ✅ LẤY SỐ TIỀN THANH TOÁN CHUẨN: GRAND TOTAL (đã trừ voucher)
        BigDecimal payAmount = order.getGrandTotal() != null ? order.getGrandTotal() : order.getTotalAmount();
        payAmount = payAmount.setScale(0, java.math.RoundingMode.DOWN);
        int amountVnd = payAmount.intValueExact(); // PayOS cần int VND

        long orderCode = generateOrderCode();

        try {
            String descRaw = "Order " + customerOrderId.toString().substring(0, 8);
            String desc = cutUtf8Bytes(asciiNoMarks(descRaw), 20);
            String itemName = cutUtf8Bytes(asciiNoMarks("Order#" + customerOrderId.toString().substring(0, 8)), 20);

            ItemData item = ItemData.builder()
                    .name(itemName)
                    .price(amountVnd)
                    .quantity(1)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(amountVnd)
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
            out.setAmount(payAmount);
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
            // ✅ Lấy đúng số tiền đã TRỪ VOUCHER
            BigDecimal amount = order.getGrandTotal() != null ? order.getGrandTotal() : order.getTotalAmount();
            if (amount == null) amount = BigDecimal.ZERO;
            amount = amount.setScale(0, java.math.RoundingMode.DOWN);

            final int amountVnd;
            try {
                amountVnd = amount.intValueExact(); // PayOS dùng int VND
            } catch (ArithmeticException ex) {
                log.error("Amount invalid (must be integer VND <= Integer.MAX_VALUE): {}", amount, ex);
                throw ex;
            }

            // 1) Ghi nhận giao dịch ví khách (idempotent theo logic của bạn)
            settlementService.recordCustomerQrPayment(order.getCustomer().getId(), order.getId(), amount);

            // 2) Platform HOLD (idempotent)
            boolean existsHolding = !platformTransactionRepository
                    .findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING)
                    .isEmpty();
            if (!existsHolding) {
                settlementService.moveToPlatformHold(order.getId(), amount);
            } else {
                log.info("Platform HOLDING already exists for orderId={} -> skip create", order.getId());
            }

            // 3) Phân bổ pending cho từng store
            settlementService.allocateToStoresPending(order);

            // 4) Cập nhật trạng thái đơn
            order.setStatus(OrderStatus.PENDING); // hoặc CONFIRMED nếu bạn muốn sau khi đã thanh toán
            order.setCreatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);

            sendPaymentSuccessEmail(order, amount);
            log.info("[PayOS Webhook] SUCCESS processed orderId={} amountVnd={}", order.getId(), amountVnd);
            return;
        }

        // thất bại/hủy
        order.setStatus(OrderStatus.UNPAID);
        order.setCreatedAt(LocalDateTime.now());
        customerOrderRepository.save(order);
        log.warn("[PayOS Webhook] FAILED/HUMAN CANCEL orderId={}", order.getId());
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
