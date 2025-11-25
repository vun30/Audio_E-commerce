package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.CustomerOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderItemResponse;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreOrderItemResponse;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.CustomerOrderItem;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
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

    private CustomerOrderDetailResponse toCustomerOrderDetail(CustomerOrder order) {

        List<CustomerOrderItemResponse> itemResponses = toCustomerOrderItemResponses(order.getItems());

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
                .items(itemResponses)
                .build();
    }

    private List<CustomerOrderItemResponse> toCustomerOrderItemResponses(List<CustomerOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(item -> CustomerOrderItemResponse.builder()
                        .id(item.getId())
                        .type(item.getType())
                        .refId(item.getRefId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .storeId(item.getStoreId())
                        // ===== Variant info =====
                        .variantId(item.getVariantId())
                        .variantOptionName(item.getVariantOptionName())
                        .variantOptionValue(item.getVariantOptionValue())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}