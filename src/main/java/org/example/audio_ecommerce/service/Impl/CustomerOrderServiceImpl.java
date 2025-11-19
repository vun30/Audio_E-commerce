package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.CustomerOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreOrderItemResponse;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.CustomerOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final StoreOrderRepository storeOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<CustomerOrderDetailResponse> getCustomerOrders(UUID customerId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomerOrder> orderPage = customerOrderRepository.findByCustomer_Id(customerId, pageable);
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

        // ✅ Đảm bảo đơn thuộc về customer đang request
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Customer does not own this order");
        }

        return toCustomerOrderDetail(order);
    }


    private CustomerOrderDetailResponse toCustomerOrderDetail(CustomerOrder order) {
        List<StoreOrder> storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(order.getId());

        List<CustomerOrderDetailResponse.StoreOrderSummary> storeSummaries = storeOrders.stream()
                .map(this::toStoreOrderSummary)
                .collect(Collectors.toList());

        return CustomerOrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .message(order.getMessage())
                .createdAt(order.getCreatedAt())
                .totalAmount(defaultBigDecimal(order.getTotalAmount()))
                .discountTotal(defaultBigDecimal(order.getDiscountTotal()))
                .shippingFeeTotal(defaultBigDecimal(order.getShippingFeeTotal()))
                .grandTotal(defaultBigDecimal(order.getGrandTotal()))
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
                .storeOrders(storeSummaries)
                .build();
    }

    private CustomerOrderDetailResponse.StoreOrderSummary toStoreOrderSummary(StoreOrder storeOrder) {
        return CustomerOrderDetailResponse.StoreOrderSummary.builder()
                .id(storeOrder.getId())
                .orderCode(storeOrder.getOrderCode())
                .storeId(storeOrder.getStore().getStoreId())
                .storeName(storeOrder.getStore().getStoreName())
                .status(storeOrder.getStatus())
                .createdAt(storeOrder.getCreatedAt())
                .totalAmount(defaultBigDecimal(storeOrder.getTotalAmount()))
                .discountTotal(defaultBigDecimal(storeOrder.getDiscountTotal()))
                .shippingFee(defaultBigDecimal(storeOrder.getShippingFee()))
                .grandTotal(defaultBigDecimal(storeOrder.getGrandTotal()))
                .items(toItemResponses(storeOrder.getItems()))
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
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}