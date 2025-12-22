package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.ProductVariantRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.CustomerOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepo;
    private final ProductVariantRepository productVariantRepo;
    private final StoreOrderRepository storeOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<CustomerOrderDetailResponse> getCustomerOrders(UUID customerId, OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomerOrder> orderPage;

        if (status != null) {
            // 👇 lọc theo status
            orderPage = customerOrderRepository.findByCustomer_IdAndStatus(customerId, status, pageable);
        } else {
            // 👇 lấy tất cả
            orderPage = customerOrderRepository.findByCustomer_Id(customerId, pageable);
        }
        List<CustomerOrderDetailResponse> items = orderPage.getContent().stream()
                .map(this::toCustomerOrderDetail)
                .collect(Collectors.toList());

        return PagedResult.<CustomerOrderDetailResponse>builder()
                .items(items)
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse getCustomerOrderDetail(UUID customerId, UUID orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Customer does not own this order");
        }

        return toCustomerOrderDetail(order);
    }



    @Override
    @Transactional
    public void confirmReceivedByCustomerOrder(UUID customerId, UUID customerOrderId) {

        CustomerOrder co = customerOrderRepository.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        if (!co.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Customer does not own this order");
        }

        StoreOrder so = storeOrderRepository.findFirstByCustomerOrder_Id(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found for this CustomerOrder"));

        if (so.getStatus() != OrderStatus.DELIVERY_SUCCESS) {
            if (so.getStatus() == OrderStatus.COMPLETED) return; // idempotent
            throw new IllegalStateException("Only DELIVERY_SUCCESS can be confirmed received");
        }

        so.setStatus(OrderStatus.COMPLETED);
        storeOrderRepository.save(so);

        co.setStatus(OrderStatus.COMPLETED);
        customerOrderRepository.save(co);
    }


    private CustomerOrderDetailResponse toCustomerOrderDetail(CustomerOrder order) {
        List<StoreOrder> storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(order.getId());

        Map<UUID, UUID> storeIdToStoreOrderId = storeOrders.stream()
                .filter(so -> so.getStore() != null && so.getStore().getStoreId() != null)
                .collect(Collectors.toMap(
                        so -> so.getStore().getStoreId(),
                        StoreOrder::getId,
                        (a, b) -> a
                ));

        List<CustomerOrderItemResponse> itemResponses =
                toCustomerOrderItemResponses(order.getItems(), storeIdToStoreOrderId);

        List<CustomerOrderItem> items = Optional.ofNullable(order.getItems()).orElse(List.of());

        // ✅ recompute theo rule: totalAmount = giá gốc (sum linePriceBeforeDiscount)
        BigDecimal recomputeTotal = items.stream()
                .map(it -> it.getLinePriceBeforeDiscount() != null ? it.getLinePriceBeforeDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =========================
        // ✅ NEW: tách giảm giá theo yêu cầu FE
        // =========================

        // 1) Voucher toàn shop (được phân bổ xuống item)
        BigDecimal storeVoucherOrderDiscount = items.stream()
                .map(it -> it.getShopOrderVoucherDiscount() != null ? it.getShopOrderVoucherDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2) Voucher theo sản phẩm của shop
        BigDecimal storeVoucherProductDiscount = items.stream()
                .map(it -> it.getShopItemDiscount() != null ? it.getShopItemDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3) (Khuyến nghị) Voucher sàn / platform (nếu có field ở item)
        BigDecimal platformVoucherDiscountTotal = items.stream()
                .map(it -> it.getPlatformVoucherDiscount() != null ? it.getPlatformVoucherDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Tổng giảm đúng bản chất: store(order) + store(product) + platform
        BigDecimal recomputeDiscountTotal = storeVoucherOrderDiscount
                .add(storeVoucherProductDiscount)
                .add(platformVoucherDiscountTotal);

        // Shipping
        BigDecimal ship = defaultBigDecimal(order.getShippingFeeTotal());

        // ✅ recompute grand theo rule: grand = total - discount + ship
        BigDecimal recomputeGrand = recomputeTotal.subtract(recomputeDiscountTotal).add(ship);
        if (recomputeGrand.compareTo(BigDecimal.ZERO) < 0) recomputeGrand = BigDecimal.ZERO;

        List<StoreOrderSummaryResponse> storeOrderResponses =
                storeOrders.stream().map(this::toStoreOrderSummary).collect(Collectors.toList());

        return CustomerOrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .message(order.getMessage())
                .createdAt(order.getCreatedAt())

                // ✅ dùng recompute thay vì order.getTotalAmount/order.getGrandTotal
                .totalAmount(recomputeTotal)

                // ✅ tổng giảm (giữ field cũ để FE không bị vỡ)
                .discountTotal(recomputeDiscountTotal)

                // ✅ NEW: tách riêng để FE hiển thị
                .storeVoucherDiscount(storeVoucherOrderDiscount)
                .storeVoucherProductDiscount(storeVoucherProductDiscount)
                .platformVoucherDiscount(platformVoucherDiscountTotal) // nếu bạn muốn hiển thị riêng

                .shippingFeeTotal(ship)
                .grandTotal(recomputeGrand)

                .externalOrderCode(order.getExternalOrderCode())
                .receiverName(order.getShipReceiverName())
                .phoneNumber(order.getShipPhoneNumber())
                .country(order.getShipCountry())
                .province(order.getShipProvince())
                .district(order.getShipDistrict())
                .ward(order.getShipWard())
                .street(order.getShipStreet())
                .addressLine(order.getShipAddressLine())
                .postalCode(order.getShipPostalCode())
                .note(order.getShipNote())
                .items(itemResponses)
                .storeOrders(storeOrderResponses)
                .build();
    }



    private List<CustomerOrderItemResponse> toCustomerOrderItemResponses(
            List<CustomerOrderItem> items,
            Map<UUID, UUID> storeIdToStoreOrderId
    ) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(item -> {
                    // ====== TÍNH image & variantUrl ======
                    String image = null;
                    String variantUrl = null;

                    if ("PRODUCT".equalsIgnoreCase(item.getType())) {
                        Product product = null;

                        // Có variantId → lấy variant, rồi lấy product từ variant
                        if (item.getVariantId() != null) {
                            ProductVariantEntity v = productVariantRepo.findById(item.getVariantId())
                                    .orElse(null);
                            if (v != null) {
                                variantUrl = v.getVariantUrl();
                                product = v.getProduct();
                            }
                        } else if (item.getRefId() != null) {
                            // Không có variant → dùng productId (refId)
                            product = productRepo.findById(item.getRefId()).orElse(null);
                        }

                        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                            image = product.getImages().get(0); // lấy ảnh đầu tiên
                        }
                    }

                    UUID storeOrderId = null;
                    if (item.getStoreId() != null) {
                        storeOrderId = storeIdToStoreOrderId.get(item.getStoreId());
                    }

                    return CustomerOrderItemResponse.builder()
                            .id(item.getId())
                            .type(item.getType())
                            .refId(item.getRefId())
                            .name(item.getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .storeId(item.getStoreId())
                            .storeOrderId(storeOrderId) // ⭐ NEW

                            // Variant info
                            .variantId(item.getVariantId())
                            .variantOptionName(item.getVariantOptionName())
                            .variantOptionValue(item.getVariantOptionValue())

                            // Image & variant url
                            .image(image)
                            .variantUrl(variantUrl)

                            .unitPriceBeforeDiscount(item.getUnitPriceBeforeDiscount())
                            .linePriceBeforeDiscount(item.getLinePriceBeforeDiscount())
                            .platformVoucherDiscount(item.getPlatformVoucherDiscount())
                            .shopItemDiscount(item.getShopItemDiscount())
                            .shopOrderVoucherDiscount(item.getShopOrderVoucherDiscount())
                            .totalItemDiscount(item.getTotalItemDiscount())
                            .finalUnitPrice(item.getFinalUnitPrice())
                            .finalLineTotal(item.getFinalLineTotal())
                            .amountCharged(item.getAmountCharged())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private StoreOrderSummaryResponse toStoreOrderSummary(StoreOrder so) {
        Store store = so.getStore();

        return StoreOrderSummaryResponse.builder()
                .id(so.getId())
                .orderCode(so.getOrderCode())
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getStoreName() : null)
                .status(so.getStatus())
                .createdAt(so.getCreatedAt())
                .deliveredAt(so.getDeliveredAt())
                .totalAmount(defaultBigDecimal(so.getTotalAmount()))
                .discountTotal(defaultBigDecimal(so.getDiscountTotal()))
                .storeVoucherDiscount(defaultBigDecimal(so.getStoreVoucherDiscount()))
                .platformVoucherDiscount(defaultBigDecimal(so.getPlatformVoucherDiscount()))
                .shippingFee(defaultBigDecimal(so.getShippingFee()))
                .shippingFeeReal(defaultBigDecimal(so.getShippingFeeReal()))
                .shippingFeeForStore(defaultBigDecimal(so.getShippingFeeForStore()))
                .grandTotal(defaultBigDecimal(so.getGrandTotal()))
                .storeVoucherDetailJson(so.getStoreVoucherDetailJson())
                .platformVoucherDetailJson(so.getPlatformVoucherDetailJson())
                .build();
    }

}