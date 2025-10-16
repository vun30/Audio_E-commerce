package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

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

        // Parse string sang enum (an toàn hơn)
        order.setStatus(status);
        storeOrderRepository.save(order);
        storeOrderRepository.flush();

        CustomerOrder customerOrder = order.getCustomerOrder();
        if (customerOrder != null) {
            var allStoreOrders = storeOrderRepository.findAllByCustomerOrder_Id(customerOrder.getId());

            boolean allCompleted = allStoreOrders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.COMPLETED);
            boolean allCancelled = allStoreOrders.stream()
                    .allMatch(o -> o.getStatus() == OrderStatus.CANCELLED);
            boolean anyShipping = allStoreOrders.stream()
                    .anyMatch(o -> o.getStatus() == OrderStatus.SHIPPING);

            OrderStatus customerNewStatus = customerOrder.getStatus();
            if (allCompleted) {
                customerNewStatus = OrderStatus.COMPLETED;
            } else if (allCancelled) {
                customerNewStatus = OrderStatus.CANCELLED;
            } else if (anyShipping) {
                customerNewStatus = OrderStatus.SHIPPING;
            } else {
                customerNewStatus = OrderStatus.AWAITING_SHIPMENT;
            }

            if (customerOrder.getStatus() != customerNewStatus) {
                customerOrder.setStatus(customerNewStatus);
                customerOrderRepository.saveAndFlush(customerOrder);
            }
        }

        return order;
    }
}
