package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.dto.response.StoreTopupResponse;
import org.example.audio_ecommerce.dto.response.WalletTopupResponse;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.email.OrderData;
import org.example.audio_ecommerce.email.OrderItemEmailData;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

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
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;
    private final PlatformWalletRepository platformWalletRepository;

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
        long amountVnd = payAmount.longValueExact();

        // v2 không giới hạn 20 bytes như v1 → nhưng cứ sanitize cho chắc
        String desc = asciiNoMarks(description != null ? description : "Group orders");
        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Group#" + batchOrderCode)
                    .price(amountVnd)
                    .quantity(1)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(batchOrderCode)   // 1 code cho cả nhóm
                    .amount(amountVnd)
                    .description(desc)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)                  // mẫu demo dùng .item(...) (không phải .items(...))
                    .build();

            CreatePaymentLinkResponse res = payOS.paymentRequests().create(paymentData);

            // GẮN batchCode vào JSON của từng order, KHÔNG đụng externalOrderCode riêng (Eᵢ)
            for (CustomerOrder order : orders) {
                if (!StringUtils.hasText(order.getExternalOrderCode())) {
                    order.setExternalOrderCode(String.valueOf(generateOrderCode()));
                }
                String json = order.getPlatformVoucherDetailJson();
                ObjectNode node = StringUtils.hasText(json) ? (ObjectNode) mapper.readTree(json) : mapper.createObjectNode();
                node.put("__payos_batch_code", String.valueOf(batchOrderCode));
                order.setPlatformVoucherDetailJson(mapper.writeValueAsString(node));

                order.setStatus(OrderStatus.PENDING);
                order.setCreatedAt(LocalDateTime.now());
                if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
                    order.setPaymentMethod(PaymentMethod.ONLINE);
                }
                customerOrderRepository.save(order);
            }

            CheckoutOnlineResponse out = new CheckoutOnlineResponse();
            out.setCustomerOrderId(orders.get(0).getId());
            out.setAmount(payAmount);
            out.setPayOSOrderCode(batchOrderCode);
            out.setCheckoutUrl(res.getCheckoutUrl()); // theo demo
            out.setQrCode(null);                       // demo không trả QR trực tiếp
            out.setStatus(OrderStatus.PENDING.name());
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Tạo link PayOS (group) thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Webhook: nhận raw body (String/Object), để SDK verify → trả về WebhookData (package vn.payos.model.webhooks).
     */
    @Override
    @Transactional
    public void confirmWebhook(WebhookData verified) {
        try { log.info("[PayOS Webhook] parsed={}", verified); } catch (Exception ignore) {}

        if (verified == null || verified.getOrderCode() == null) {
            log.error("[PayOS Webhook] orderCode null. webhook={}", verified);
            return;
        }

        Long code = verified.getOrderCode();
        String resultCode = verified.getCode();                 // "00" = success
        boolean success = "00".equals(resultCode);
        String desc = verified.getDesc() != null ? verified.getDesc() : verified.getDescription();

        // ========= 1) Thử xử lý như batch order e-commerce =========
        List<CustomerOrder> all = customerOrderRepository.findAll();
        List<CustomerOrder> orders = new ArrayList<>();
        for (CustomerOrder o : all) {
            String json = o.getPlatformVoucherDetailJson();
            if (!StringUtils.hasText(json)) continue;
            try {
                var node = mapper.readTree(json);
                if (node.has("__payos_batch_code")
                        && String.valueOf(code).equals(node.get("__payos_batch_code").asText())) {
                    orders.add(o);
                }
            } catch (Exception ignored) {}
        }

        if (!orders.isEmpty()) {
            handleEcomOrdersWebhook(orders, code, success, desc);
            return;
        }

        // ========= 2) Không tìm thấy batch order => thử xem có phải topup ví =========
        walletTransactionRepository.findByExternalRef(String.valueOf(code))
                .ifPresentOrElse(
                        txn -> handleWalletTopupWebhook(txn, success, desc),
                        () -> storeWalletTransactionRepository.findByExternalRef(String.valueOf(code))
                                .ifPresentOrElse(
                                        stxn -> handleStoreWalletTopupWebhook(stxn, success, desc),
                                        () -> log.error("[PayOS Webhook] No batch, no WalletTransaction, no StoreWalletTransaction for code={}", code)
                                )
                );

    }


    @Override
    @Transactional
    public WalletTopupResponse createWalletTopupPayment(
            UUID customerId,
            BigDecimal amount,
            String description,
            String returnUrl,
            String cancelUrl
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        Wallet wallet = walletRepository.findByCustomer_Id(customerId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for customerId=" + customerId));

        BigDecimal topupAmount = amount.setScale(0, BigDecimal.ROUND_DOWN);
        long amountVnd = topupAmount.longValueExact();

        long orderCode = generateOrderCode(); // dùng luôn hàm có sẵn

        String desc = asciiNoMarks(
                description != null ? description : ("Wallet topup " + customerId));

        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("WalletTopup#" + orderCode)
                    .price(amountVnd)
                    .quantity(1)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amountVnd)
                    .description(desc)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CreatePaymentLinkResponse res = payOS.paymentRequests().create(paymentData);

            // Tạo transaction PENDING
            BigDecimal before = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

            WalletTransaction txn = WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(topupAmount)
                    .transactionType(WalletTransactionType.TOPUP)
                    .status(WalletTransactionStatus.PENDING)
                    .description("Top up via PayOS, orderCode=" + orderCode)
                    .balanceBefore(before)
                    .balanceAfter(before) // chưa cộng tiền
                    .orderId(null)
                    .externalRef(String.valueOf(orderCode))
                    .build();

            walletTransactionRepository.save(txn);

            WalletTopupResponse out = new WalletTopupResponse();
            out.setWalletTransactionId(txn.getId());
            out.setAmount(topupAmount);
            out.setPayOSOrderCode(orderCode);
            out.setCheckoutUrl(res.getCheckoutUrl());
            out.setStatus(WalletTransactionStatus.PENDING.name());

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Tạo link PayOS topup ví thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public StoreTopupResponse createStoreWalletTopupPayment(
            UUID storeId,
            BigDecimal amount,
            String description,
            String returnUrl,
            String cancelUrl
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new NoSuchElementException("StoreWallet not found for storeId=" + storeId));

        BigDecimal topupAmount = amount.setScale(0, java.math.RoundingMode.DOWN);
        long amountVnd = topupAmount.longValueExact();

        long orderCode = generateOrderCode();

        String desc = asciiNoMarks(description != null ? description : ("Store topup " + storeId));

        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("StoreTopup#" + orderCode)
                    .price(amountVnd)
                    .quantity(1)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amountVnd)
                    .description(desc)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CreatePaymentLinkResponse res = payOS.paymentRequests().create(paymentData);

            BigDecimal before = wallet.getDefaultBalance() != null ? wallet.getDefaultBalance() : BigDecimal.ZERO;

            StoreWalletTransaction txn = StoreWalletTransaction.builder()
                    .wallet(wallet)
                    .amount(topupAmount)
                    .type(StoreWalletTransactionType.TOPUP)
                    .status(StoreWalletTransactionStatus.PENDING)
                    .description("Store topup via PayOS, orderCode=" + orderCode)
                    .balanceBefore(before)
                    .balanceAfter(before) // chưa cộng
                    .orderId(null)
                    .externalRef(String.valueOf(orderCode))
                    .createdAt(LocalDateTime.now())
                    .build();

            storeWalletTransactionRepository.save(txn);

            return StoreTopupResponse.builder()
                    .transactionId(txn.getTransactionId())
                    .amount(topupAmount)
                    .payOSOrderCode(orderCode)
                    .checkoutUrl(res.getCheckoutUrl())
                    .status(txn.getStatus().name())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Tạo link PayOS store topup thất bại: " + e.getMessage(), e);
        }
    }


    /* ================= helpers ================= */

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
        if (!StringUtils.hasText(value)) return;
        String sanitized = value.trim();
        if (!parts.contains(sanitized)) parts.add(sanitized);
    }

    private static String asciiNoMarks(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "");
    }

    @SuppressWarnings("unused")
    private static String cutUtf8Bytes(String s, int maxBytes) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (b.length <= maxBytes) return s;
        int end = maxBytes;
        while (end > 0 && (b[end] & 0xC0) == 0x80) end--;
        return new String(b, 0, end, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void handleEcomOrdersWebhook(List<CustomerOrder> orders,
                                         Long batchCode,
                                         boolean success,
                                         String desc) {

        log.info("[PayOS Webhook][ECOM] batch={} size={} success={} desc={}",
                batchCode, orders.size(), success, desc);

        if (desc != null && desc.toLowerCase().contains("hết hạn")) {
            for (CustomerOrder order : orders) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCreatedAt(LocalDateTime.now());
                customerOrderRepository.save(order);
            }
            return;
        }

        if (success) {
            for (CustomerOrder order : orders) {

                BigDecimal amount = order.getGrandTotal() != null ? order.getGrandTotal() : order.getTotalAmount();
                if (amount == null) amount = BigDecimal.ZERO;
                amount = amount.setScale(0, java.math.RoundingMode.DOWN);

                // (Tuỳ bạn) vẫn có thể giữ recordCustomerQrPayment nếu nó CHỈ set trạng thái đã thanh toán
                // Nếu recordCustomerQrPayment đang tạo HOLD/PENDING thì nên bỏ luôn.
                settlementService.recordCustomerQrPayment(order.getCustomer().getId(), order.getId(), amount);

                // ✅ NEW: ONLINE PAY -> chỉ CASH IN vào PlatformWallet.cashBalance
                // Idempotent theo orderId (tránh webhook bắn lại cộng tiền 2 lần)
                String idemKey = "PAYOS:ECOM:CASHIN:" + order.getId();
                boolean existed = platformTransactionRepository
                        .findByIdempotencyKey(idemKey)
                        .isPresent();
                if (!existed) {
                    cashInToPlatformForOnlineOrder(order, amount, idemKey);
                }

                // ❌ BỎ 2 dòng này (vì nó đẩy tiền qua pending platform + pending store)
                // settlementService.moveToPlatformHold(order.getId(), amount);
                // settlementService.allocateToStoresPending(order);

                order.setStatus(OrderStatus.PENDING); // hoặc PAID nếu bạn có enum
                order.setCreatedAt(LocalDateTime.now());
                customerOrderRepository.save(order);

                sendPaymentSuccessEmail(order, amount);
            }
            log.info("[PayOS Webhook][ECOM] SUCCESS processed batch={} orders={}", batchCode, orders.size());
            return;
        }


        for (CustomerOrder order : orders) {
            order.setStatus(OrderStatus.UNPAID);
            order.setCreatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);
        }
    }

    private void handleWalletTopupWebhook(WalletTransaction txn,
                                          boolean success,
                                          String desc) {
        log.info("[PayOS Webhook][WALLET] txnId={} extRef={} success={} desc={}",
                txn.getId(), txn.getExternalRef(), success, desc);

        // Idempotent: nếu đã SUCCESS thì bỏ qua
        if (txn.getStatus() == WalletTransactionStatus.SUCCESS) {
            log.info("[PayOS Webhook][WALLET] Txn already SUCCESS, skip.");
            return;
        }

        if (!success) {
            txn.setStatus(WalletTransactionStatus.FAILED);
            walletTransactionRepository.save(txn);
            return;
        }

        Wallet wallet = txn.getWallet();
        BigDecimal before = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(txn.getAmount());

        wallet.setBalance(after);
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // ===== PLATFORM CASH IN (CUSTOMER TOPUP) =====
        PlatformWallet platform = platformWalletRepository.getPlatformMainWallet();

        BigDecimal cashBefore = platform.getCashBalance() != null ? platform.getCashBalance() : BigDecimal.ZERO;
        BigDecimal cashAfter  = cashBefore.add(txn.getAmount());

        platform.setCashBalance(cashAfter);
        platform.setReceivedTotal(
                (platform.getReceivedTotal() != null ? platform.getReceivedTotal() : BigDecimal.ZERO)
                        .add(txn.getAmount())
        );
        platformWalletRepository.save(platform);

// Lưu platform_transaction (bucket=CASH)
        PlatformTransaction pTxn = PlatformTransaction.builder()
                .wallet(platform)
                .orderId(null)
                .storeId(null)
                .customerId(wallet.getCustomer().getId())          // hoặc customerId param nếu bạn có
                .amount(txn.getAmount())
                .type(TransactionType.TOPUP)                       // nếu enum bạn dùng tên khác thì đổi đúng tên
                .status(TransactionStatus.DONE)
                .description("Customer topup via PayOS, walletTxnId=" + txn.getId())
                .idempotencyKey("PAYOS:WALLET_TOPUP:" + txn.getExternalRef()) // EXTREF = orderCode PayOS
                .channel(PaymentChannel.PAYOS)
                .externalRefId(null)
                .externalRefCode(txn.getExternalRef())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .bucket(WalletBucket.CASH)
                .direction(TxDirection.IN)
                .balanceBefore(cashBefore)
                .balanceAfter(cashAfter)
                .build();

        platformTransactionRepository.save(pTxn);



        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setStatus(WalletTransactionStatus.SUCCESS);
        walletTransactionRepository.save(txn);

        log.info("[PayOS Webhook][WALLET] SUCCESS topup walletId={} amount={} before={} after={}",
                wallet.getId(), txn.getAmount(), before, after);
    }

    private void handleStoreWalletTopupWebhook(StoreWalletTransaction txn,
                                               boolean success,
                                               String desc) {
        log.info("[PayOS Webhook][STORE_WALLET] txnId={} extRef={} success={} desc={}",
                txn.getTransactionId(), txn.getExternalRef(), success, desc);

        // Idempotent
        if (txn.getStatus() == StoreWalletTransactionStatus.SUCCESS) {
            log.info("[PayOS Webhook][STORE_WALLET] Txn already SUCCESS, skip.");
            return;
        }

        if (!success) {
            txn.setStatus(StoreWalletTransactionStatus.FAILED);
            storeWalletTransactionRepository.save(txn);
            return;
        }

        StoreWallet wallet = txn.getWallet();
        BigDecimal before = wallet.getDefaultBalance() != null ? wallet.getDefaultBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(txn.getAmount());

        wallet.setDefaultBalance(after);
        wallet.setUpdatedAt(LocalDateTime.now());
        storeWalletRepository.save(wallet);

        // ===== PLATFORM CASH IN (CUSTOMER TOPUP) =====
        PlatformWallet platform = platformWalletRepository.getPlatformMainWallet();

        BigDecimal cashBefore = platform.getCashBalance() != null ? platform.getCashBalance() : BigDecimal.ZERO;
        BigDecimal cashAfter  = cashBefore.add(txn.getAmount());

        platform.setCashBalance(cashAfter);
        platform.setReceivedTotal(
                (platform.getReceivedTotal() != null ? platform.getReceivedTotal() : BigDecimal.ZERO)
                        .add(txn.getAmount())
        );
        platformWalletRepository.save(platform);

// Lưu platform_transaction (bucket=CASH)
        PlatformTransaction pTxn = PlatformTransaction.builder()
                .wallet(platform)
                .orderId(null)
                .storeId(wallet.getStore().getStoreId())          // hoặc customerId param nếu bạn có
                .amount(txn.getAmount())
                .type(TransactionType.TOPUP)                       // nếu enum bạn dùng tên khác thì đổi đúng tên
                .status(TransactionStatus.DONE)
                .description("Store topup via PayOS, walletTxnId=" + txn.getOrderId())
                .idempotencyKey("PAYOS:WALLET_TOPUP:" + txn.getExternalRef()) // EXTREF = orderCode PayOS
                .channel(PaymentChannel.PAYOS)
                .externalRefId(null)
                .externalRefCode(txn.getExternalRef())
                .bucket(WalletBucket.CASH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .direction(TxDirection.IN)
                .balanceBefore(cashBefore)
                .balanceAfter(cashAfter)
                .build();

        platformTransactionRepository.save(pTxn);


        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setStatus(StoreWalletTransactionStatus.SUCCESS);
        storeWalletTransactionRepository.save(txn);

        log.info("[PayOS Webhook][STORE_WALLET] SUCCESS topup walletId={} amount={} before={} after={}",
                wallet.getWalletId(), txn.getAmount(), before, after);
    }

    //helper lưu vào cashblance
    private void cashInToPlatformForOnlineOrder(CustomerOrder order,
                                                BigDecimal amount,
                                                String idempotencyKey) {

        PlatformWallet platform = platformWalletRepository.getPlatformMainWallet();

        BigDecimal cashBefore = platform.getCashBalance() != null ? platform.getCashBalance() : BigDecimal.ZERO;
        BigDecimal cashAfter  = cashBefore.add(amount);

        platform.setCashBalance(cashAfter);

        // bạn đang có receivedTotal trong PlatformWallet -> cộng luôn cho đúng nghĩa "tiền đã nhận"
        platform.setReceivedTotal(
                (platform.getReceivedTotal() != null ? platform.getReceivedTotal() : BigDecimal.ZERO)
                        .add(amount)
        );

        platform.setUpdatedAt(LocalDateTime.now());
        platformWalletRepository.save(platform);

        // ✅ lưu ledger platform_transaction (bucket=CASH)
        PlatformTransaction pTxn = PlatformTransaction.builder()
                .wallet(platform)
                .orderId(order.getId())
                .storeId(null) // online multi-store, store payout xử lý sau (khi delivered/settlement)
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .amount(amount)
                .type(TransactionType.HOLD)       // đổi đúng enum của bạn (vd: PAYMENT/ORDER_PAYMENT)
                .status(TransactionStatus.DONE)      // vì CASH đã vào platform
                .description("PayOS online payment cash-in, orderId=" + order.getId())
                .idempotencyKey(idempotencyKey)
                .channel(PaymentChannel.PAYOS)
                .externalRefId(null)
                .externalRefCode(order.getExternalOrderCode()) // hoặc batchCode tuỳ bạn muốn trace
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .bucket(WalletBucket.CASH)
                .direction(TxDirection.IN)
                .balanceBefore(cashBefore)
                .balanceAfter(cashAfter)
                .build();

        platformTransactionRepository.save(pTxn);
    }

    @Transactional
    public void cancelWalletTopup(String externalRef) {

        WalletTransaction txn = walletTransactionRepository
                .findByExternalRef(externalRef)
                .orElseThrow(() -> new NoSuchElementException("WalletTransaction not found"));

        if (txn.getStatus() != WalletTransactionStatus.PENDING) return;

        txn.setStatus(WalletTransactionStatus.CANCELLED);
        txn.setUpdatedAt(LocalDateTime.now());
        walletTransactionRepository.save(txn);
    }

    @Transactional
    public void cancelStoreWalletTopup(String externalRef) {

        StoreWalletTransaction txn = storeWalletTransactionRepository
                .findByExternalRef(externalRef)
                .orElseThrow(() -> new NoSuchElementException("StoreWalletTransaction not found"));

        if (txn.getStatus() != StoreWalletTransactionStatus.PENDING) return;

        txn.setStatus(StoreWalletTransactionStatus.CANCELLED);
        txn.setCreatedAt(LocalDateTime.now());
        storeWalletTransactionRepository.save(txn);
    }

    @Transactional
    public void cancelOnlineOrderByBatchCode(Long batchCode) {

        List<CustomerOrder> orders = customerOrderRepository.findAll();
        for (CustomerOrder o : orders) {
            if (!o.getPlatformVoucherDetailJson().contains(batchCode.toString())) continue;

            if (o.getStatus() == OrderStatus.PENDING) {
                o.setStatus(OrderStatus.CANCELLED);
                o.setCreatedAt(LocalDateTime.now());
                customerOrderRepository.save(o);
            }
        }
    }

}
