package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.StoreOrderItemResponse;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        storeOrderRepository.flush();

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
    public PagedResult<StoreOrderDetailResponse> getOrdersForStore(UUID storeId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<StoreOrder> ordersPage = storeOrderRepository.findByStore_StoreId(storeId, pageable);
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

    private StoreOrderDetailResponse toDetailResponse(StoreOrder order) {
        CustomerOrder customerOrder = order.getCustomerOrder();
        Customer customer = customerOrder != null ? customerOrder.getCustomer() : null;

        return StoreOrderDetailResponse.builder()
                .id(order.getId())
                .storeId(order.getStore().getStoreId())
                .storeName(order.getStore().getStoreName())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .totalAmount(defaultBigDecimal(order.getTotalAmount()))
                .discountTotal(defaultBigDecimal(order.getDiscountTotal()))
                .shippingFee(defaultBigDecimal(order.getShippingFee()))
                .grandTotal(defaultBigDecimal(order.getGrandTotal()))
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
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
