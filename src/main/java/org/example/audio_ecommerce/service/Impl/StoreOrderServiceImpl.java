package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionStatus;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
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

    @Override
    @Transactional
    public StoreOrder updateOrderStatus(UUID storeId, UUID orderId, OrderStatus status) {
        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        // Cập nhật status cho StoreOrder
        order.setStatus(status);

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

        return getOrderDetailForStore(storeId, orderId);
    }

    /**
     * Refund tiền về ví khách (Wallet.balance) + lưu WalletTransaction.
     * Có idempotency để chống hoàn trùng bằng externalRef.
     */
    private void refundToCustomerWalletForStoreCancel(CustomerOrder co, String reason) {

        // 1) xác định customerId
        UUID customerId = null;

        // OPTION A: co.getCustomer().getId()
        if (co.getCustomer() != null) {
            customerId = co.getCustomer().getId();
        }

        // OPTION B: nếu bạn có co.getCustomerId()
        // customerId = co.getCustomerId();

        if (customerId == null) {
            throw new IllegalStateException("Cannot resolve customerId for refund");
        }

        // 2) xác định số tiền refund
        // ✅ Bạn cần chọn field đúng của bạn:
        // - Nếu online thu đúng tổng tiền: dùng co.getGrandTotal()
        // - Hoặc dùng co.getPaidAmount() nếu bạn có field này
        BigDecimal refundAmount = co.getGrandTotal();
        // BigDecimal refundAmount = co.getPaidAmount();

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // không có gì để refund
        }

        // 3) idempotency key chống refund trùng
        // (1 order chỉ refund 1 lần cho action STORE_CANCEL)
        String externalRef = "REFUND:STORE_CANCEL:" + co.getId();

        boolean existed = walletTransactionRepository.existsByExternalRef(externalRef);
        if (existed) {
            return; // đã refund rồi
        }

        // 4) lấy ví khách (nếu muốn chống race condition, bạn nên lock row)
        Wallet wallet = walletRepository.findByCustomer_Id(customerId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for customer"));

        BigDecimal before = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(refundAmount);

        wallet.setBalance(after);
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // 5) lưu transaction
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(refundAmount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description(buildRefundDescription(co.getId(), reason))
                .balanceBefore(before)
                .balanceAfter(after)
                .orderId(co.getId())
                .externalRef(externalRef)
                .build();

        walletTransactionRepository.save(tx);
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
                .paymentMethod(order.getPaymentMethod())
                .customerOrderId(customerOrder != null ? customerOrder.getId() : null)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerPhone(customer != null ? customer.getPhoneNumber() : null)
                .customerMessage(customerOrder != null ? customerOrder.getMessage() : null)
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
