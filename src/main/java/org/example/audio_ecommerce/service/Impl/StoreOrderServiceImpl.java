package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.NotificationCreatorService;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class StoreOrderServiceImpl implements StoreOrderService {

    private final StoreOrderRepository storeOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final NotificationCreatorService notificationCreatorService;

    @Override
    @Transactional
    public StoreOrder updateOrderStatus(UUID storeId, UUID orderId, OrderStatus status) {
        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        Store store = order.getStore();

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Store is not ACTIVE. Current status = " + store.getStatus()
            );
        }

        // Cập nhật status cho StoreOrder
        order.setStatus(status);

        if (status == OrderStatus.AWAITING_SHIPMENT) {
            if (order.getConfirmedAt() == null) { // chống set lại nhiều lần
                order.setConfirmedAt(LocalDateTime.now());
            }
        }

        // ✅ Nếu store-order chuyển sang DELIVERY_SUCCESS → set deliveredAt
        if (status == OrderStatus.DELIVERY_SUCCESS) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        storeOrderRepository.save(order);
//        storeOrderRepository.flush();

        // ====== Đồng bộ trạng thái & deliveredAt cho CustomerOrder ======
        CustomerOrder customerOrder = order.getCustomerOrder();
        if (customerOrder != null) {
            var allStoreOrders = storeOrderRepository.findAllByCustomerOrder_Id(customerOrder.getId());

            boolean allDelivered = allStoreOrders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.DELIVERY_SUCCESS);

            boolean allCompleted = allStoreOrders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.COMPLETED);

            boolean allCancelled = allStoreOrders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.CANCELLED);

            boolean anyShipping = allStoreOrders.stream()
                    .anyMatch(o -> o.getStatus() == OrderStatus.SHIPPING);

            OrderStatus customerNewStatus = customerOrder.getStatus();

            // 👇 Ưu tiên DELIVERY_SUCCESS nếu tất cả store-order đã giao xong
            if (allDelivered) {
                customerNewStatus = OrderStatus.DELIVERY_SUCCESS;
            } else if (allCompleted) {
                customerNewStatus = OrderStatus.COMPLETED;
            } else if (allCancelled) {
                customerNewStatus = OrderStatus.CANCELLED;
            } else if (anyShipping) {
                customerNewStatus = OrderStatus.SHIPPING;
            } else {
                customerNewStatus = OrderStatus.AWAITING_SHIPMENT;
            }

            // Nếu status CustomerOrder thay đổi → set deliveredAt nếu là DELIVERY_SUCCESS
            if (customerOrder.getStatus() != customerNewStatus) {
                customerOrder.setStatus(customerNewStatus);

                if (customerNewStatus == OrderStatus.DELIVERY_SUCCESS) {
                    // ✅ Khi toàn bộ store-order đã DELIVERY_SUCCESS → set deliveredAt cho CustomerOrder
                    customerOrder.setDeliveredAt(LocalDateTime.now());
                }

                customerOrderRepository.saveAndFlush(customerOrder);
            }
        }

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<StoreOrderDetailResponse> getOrdersForStore(
            UUID storeId,
            int page,
            int size,
            String orderCodeKeyword,
            OrderStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // chuyển LocalDate -> LocalDateTime
        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;
        if (fromDate != null) {
            fromDateTime = fromDate.atStartOfDay();
        }
        if (toDate != null) {
            // lấy hết ngày toDate -> +1 ngày rồi < ...
            toDateTime = toDate.plusDays(1).atStartOfDay();
        }

        Page<StoreOrder> ordersPage = storeOrderRepository.searchStoreOrders(
                storeId,
                orderCodeKeyword,
                status,
                fromDateTime,
                toDateTime,
                pageable
        );

        List<StoreOrderDetailResponse> items = ordersPage.getContent().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        return PagedResult.<StoreOrderDetailResponse>builder()
                .items(items)
                .totalElements(ordersPage.getTotalElements())
                .totalPages(ordersPage.getTotalPages())
                .page(ordersPage.getNumber())
                .size(ordersPage.getSize())
                .build();
    }



    @Override
    @Transactional(readOnly = true)
    public StoreOrderDetailResponse getOrderDetailForStore(UUID storeId, UUID orderId) {
        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        // đảm bảo đơn này thuộc về store đang request
        if (!order.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        return toDetailResponse(order);
    }

    @Override
    @Transactional
    public StoreOrderDetailResponse cancelNewOrder(UUID storeId, UUID orderId, String reason) {
        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (order.getStore() == null || order.getStore().getStoreId() == null) {
            throw new IllegalStateException("Order missing store info");
        }

        if (!order.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        // 1) Chỉ cho huỷ khi “mới nhận”
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled by store");
        }

        // 2) Lấy CustomerOrder + payment method
        // ✅ Nếu StoreOrder của bạn có relation: order.getCustomerOrder() thì dùng luôn.
        // ❗ Nếu không có relation, bạn phải query theo customerOrderId.
        CustomerOrder co = null;

        // --- OPTION A: có quan hệ trực tiếp ---
        // co = order.getCustomerOrder();

        // --- OPTION B: không có quan hệ, có field customerOrderId ---
        // co = customerOrderRepository.findById(order.getCustomerOrderId())
        //         .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        // --- OPTION C: nếu StoreOrder có field customerOrder (nhưng bạn chưa chắc) ---
        // cứ thử lấy qua getter, nếu compile fail thì bạn dùng OPTION B ở trên
        try {
            co = order.getCustomerOrder();
        } catch (Exception ignore) {
            // ignore
        }

        if (co == null) {
            throw new IllegalStateException("Cannot resolve CustomerOrder from StoreOrder. Please map relation or query by customerOrderId.");
        }

        PaymentMethod paymentMethod = co.getPaymentMethod();
        // Nếu paymentMethod của bạn là String thì thay bằng:
        // String paymentMethod = co.getPaymentMethod();

        // 3) Set CANCELLED + sync CustomerOrder bằng logic sẵn có
        updateOrderStatus(storeId, orderId, OrderStatus.CANCELLED);

        // 4) Nhánh COD vs ONLINE
        if (paymentMethod == PaymentMethod.COD) {
            // COD: trừ legalPoint bình thường (không âm)
            minusLegalPointNonNegative(order.getStore(), 1);
            storeRepository.save(order.getStore());
        } else {
            // ONLINE: hoàn tiền về ví khách
            refundToCustomerWalletForStoreCancel(co, reason);
        }

        CustomerOrder customerOrder = order.getCustomerOrder();
        notificationCreatorService.createAndSend(
                NotificationTarget.CUSTOMER,
                customerOrder.getCustomer().getId(),
                NotificationType.ORDER_CANCEL_APPROVED, // gợi ý enum
                "Đơn hàng " + customerOrder.getOrderCode() + " đã bị huỷ",
                "Cửa hàng đã huỷ đơn hàng của bạn với lý do" + reason + ". Số tiền tương ứng sẽ được hoàn về ví của bạn.",
                "/customer/orders/" + customerOrder.getId(),
                "{\"customerOrderId\":\"" + customerOrder.getId() + "\",\"storeOrderId\":\"" + order.getId() + "\"}",
                Map.of(
                        "screen", "ORDER_DETAIL",
                        "customerOrderId", String.valueOf(customerOrder.getId()),
                        "storeOrderId", String.valueOf(order.getId())
                )
        );

        return getOrderDetailForStore(storeId, orderId);
    }

    /**
     * ONLINE: Refund từ PlatformWallet.cashBalance -> Customer Wallet
     * + ghi PlatformTransaction + WalletTransaction
     * Idempotent theo:
     *  - platform_transaction.idempotencyKey
     *  - wallet_transactions.external_ref
     */
    private void refundToCustomerWalletForStoreCancel(CustomerOrder co, String reason) {

        // 0) customerId
        if (co.getCustomer() == null || co.getCustomer().getId() == null) {
            throw new IllegalStateException("Cannot resolve customerId for refund");
        }
        UUID customerId = co.getCustomer().getId();

        // 1) amount refund (online thu tổng)
        BigDecimal refundAmount = co.getGrandTotal();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 2) Idempotency keys
        String walletExternalRef = "REFUND:STORE_CANCEL:" + co.getId();
        String platformIdemKey   = "PLAT:REFUND:STORE_CANCEL:" + co.getId();

        // Nếu đã refund ví khách rồi -> coi như xong (tránh cộng trùng)
        boolean walletTxExisted = walletTransactionRepository.existsByExternalRef(walletExternalRef);
        if (walletTxExisted) {
            return;
        }

        // Nếu đã có platform tx -> cũng coi là xong (tránh trừ cashBalance trùng)
        // (Bạn cần method existsByIdempotencyKey trong PlatformTransactionRepository)
        boolean platTxExisted = platformTransactionRepository.existsByIdempotencyKey(platformIdemKey);
        if (platTxExisted) {
            return;
        }

        // 3) Load platform wallet
        PlatformWallet plat = platformWalletRepository.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // 4) Trừ tiền từ cashBalance (không cho âm)
        BigDecimal beforeCash = nz(plat.getCashBalance());
        BigDecimal afterCash  = beforeCash.subtract(refundAmount);
        if (afterCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Platform cashBalance is insufficient for refund. cash=" + beforeCash + ", refund=" + refundAmount);
        }

        plat.setCashBalance(afterCash);
        // Optional: track total refunded
        plat.setRefundedTotal(nz(plat.getRefundedTotal()).add(refundAmount));
        plat.setUpdatedAt(LocalDateTime.now());
        platformWalletRepository.save(plat);

        // 5) Ghi PlatformTransaction (đủ NOT NULL fields)
        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(co.getId())
                .customerId(customerId)
                .amount(refundAmount)

                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .channel(PaymentChannel.PAYOS)          // đổi nếu enum bạn khác
                .bucket(WalletBucket.CASH)             // nếu bạn không có CASH, dùng bucket phù hợp (VD: PENDING/AVAILABLE)
                .direction(TxDirection.OUT)

                .balanceBefore(beforeCash)
                .balanceAfter(afterCash)

                .description(buildRefundDescription(co.getId(), reason))
                .idempotencyKey(platformIdemKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        platformTransactionRepository.save(ptx);

        // 6) Cộng tiền vào ví khách + WalletTransaction
        Wallet wallet = walletRepository.findByCustomer_Id(customerId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for customer"));

        BigDecimal before = nz(wallet.getBalance());
        BigDecimal after  = before.add(refundAmount);

        wallet.setBalance(after);
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(refundAmount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description(buildRefundDescription(co.getId(), reason))
                .balanceBefore(before)
                .balanceAfter(after)
                .orderId(co.getId())
                .externalRef(walletExternalRef)
                .build();

        walletTransactionRepository.save(tx);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }


    private String buildRefundDescription(UUID orderId, String reason) {
        String base = "Refund for store-cancelled order " + orderId;
        if (reason == null || reason.isBlank()) return base;
        return base + " | reason=" + reason;
    }

    /**
     * Trừ legalPoint nhưng không bao giờ âm.
     */
    private void minusLegalPointNonNegative(Store store, int point) {
        if (store == null) return;

        BigDecimal current = store.getLegalPoint();
        if (current == null) current = BigDecimal.ZERO;

        if (current.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal next = current.subtract(BigDecimal.valueOf(point));
        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;

        store.setLegalPoint(next);
    }

    private StoreOrderDetailResponse toDetailResponse(StoreOrder order) {
        CustomerOrder customerOrder = order.getCustomerOrder();
        Customer customer = customerOrder != null ? customerOrder.getCustomer() : null;

        return StoreOrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .storeId(order.getStore().getStoreId())
                .storeName(order.getStore().getStoreName())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .totalAmount(defaultBigDecimal(order.getTotalAmount()))
                .discountTotal(defaultBigDecimal(order.getDiscountTotal()))
                .shippingFee(defaultBigDecimal(order.getShippingFee()))
                .grandTotal(defaultBigDecimal(order.getGrandTotal()))
                .shopVouchers(parseShopVouchers(order))
                .confirmedAt(order.getConfirmedAt())
                .paymentMethod(order.getPaymentMethod())
                .customerOrderId(customerOrder != null ? customerOrder.getId() : null)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerPhone(customer != null ? customer.getPhoneNumber() : null)
                .customerMessage(customerOrder != null ? customerOrder.getMessage() : null)
                .platformFeeAmount(defaultBigDecimal(order.getPlatformFeeAmount()))
                .platformFeePercentage(defaultBigDecimal(order.getPlatformFeePercentage()))
                .netPayoutToStore(defaultBigDecimal(order.getNetPayoutToStore()))
                .shipReceiverName(order.getShipReceiverName())
                .shipPhoneNumber(order.getShipPhoneNumber())
                .shipCountry(order.getShipCountry())
                .shipProvince(order.getShipProvince())
                .shipDistrict(order.getShipDistrict())
                .shipWard(order.getShipWard())
                .shipStreet(order.getShipStreet())
                .shipAddressLine(order.getShipAddressLine())
                .shipPostalCode(order.getShipPostalCode())
                .shipNote(order.getShipNote())
                .items(toItemResponses(order.getItems()))
                .build();
    }

    private List<StoreOrderItemResponse> toItemResponses(List<StoreOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> StoreOrderItemResponse.builder()
                        .id(item.getId())
                        .type(item.getType())
                        .refId(item.getRefId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        // Snapshot pricing
                        .unitPriceBeforeDiscount(item.getUnitPriceBeforeDiscount())
                        .linePriceBeforeDiscount(item.getLinePriceBeforeDiscount())
                        .platformVoucherDiscount(item.getPlatformVoucherDiscount())
                        .shopItemDiscount(item.getShopItemDiscount())
                        .shopOrderVoucherDiscount(item.getShopOrderVoucherDiscount())
                        .totalItemDiscount(item.getTotalItemDiscount())
                        .finalUnitPrice(item.getFinalUnitPrice())
                        .finalLineTotal(item.getFinalLineTotal())
                        .amountCharged(item.getAmountCharged())
                        // Legacy
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .eligibleForPayout(item.getEligibleForPayout())
                        .isPayout(item.getIsPayout())
                        .isReturned(item.getIsReturned())

                        .shippingFeeEstimated(item.getShippingFeeEstimated())
                        .shippingFeeActual(item.getShippingFeeActual())
                        .shippingExtraForStore(item.getShippingExtraForStore())

                        .platformFeeAmount(item.getPlatformFeeAmount())
                        .netPayoutItem(item.getNetPayoutItem())

                        .payoutProcessed(item.getPayoutProcessed())

                        .platformFeePercentage(item.getPlatformFeePercentage())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<ShopVoucherAppliedResponse> parseShopVouchers(StoreOrder order) {
        if (order == null) return Collections.emptyList();

        String json = order.getStoreVoucherDetailJson();
        BigDecimal totalDiscount = order.getStoreVoucherDiscount(); // tổng: 18600

        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            // không có detail json, chỉ có tổng (nếu có)
            if (totalDiscount == null || totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                return Collections.emptyList();
            }
            return List.of(ShopVoucherAppliedResponse.builder()
                    .code(null)
                    .discount(defaultBigDecimal(totalDiscount))
                    .build());
        }

        try {
            JsonNode node = objectMapper.readTree(json);

            // ✅ Case map: {"CODE1":6000,"CODE2":12600}
            if (node != null && node.isObject()) {
                List<ShopVoucherAppliedResponse> list = new java.util.ArrayList<>();

                node.fields().forEachRemaining(entry -> {
                    String code = entry.getKey();
                    JsonNode v = entry.getValue();

                    BigDecimal amount = BigDecimal.ZERO;
                    if (v != null && v.isNumber()) amount = v.decimalValue();
                    else if (v != null && v.isTextual()) {
                        try { amount = new BigDecimal(v.asText()); } catch (Exception ignore) {}
                    }

                    list.add(ShopVoucherAppliedResponse.builder()
                            .code(code)
                            .discount(amount)
                            .build());
                });

                // (tuỳ chọn) nếu muốn đảm bảo sum(list) == storeVoucherDiscount thì có thể normalize
                return list;
            }

            // ✅ Nếu sau này bạn đổi format thành array/object khác thì xử lý thêm ở đây.

        } catch (Exception ignore) {
            // ignore
        }

        // fallback: nếu parse fail thì vẫn trả tổng discount
        if (totalDiscount == null || totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }
        return List.of(ShopVoucherAppliedResponse.builder()
                .discount(defaultBigDecimal(totalDiscount))
                .build());
    }


}
