package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreOrderServiceImpl implements StoreOrderService {
    private final StoreOrderRepository storeOrderRepository;
    private final StoreRepository storeRepository;
    private final CustomerOrderRepository customerOrderRepository;

    @Override
    @Transactional
    public StoreOrder updateOrderStatus(UUID storeId, UUID orderId, String status) {
        StoreOrder order = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        order.setStatus(status.trim().toUpperCase());
        storeOrderRepository.save(order);
        storeOrderRepository.flush(); // đảm bảo cập nhật ngay

        CustomerOrder customerOrder = order.getCustomerOrder();
        if (customerOrder != null) {
            var allStoreOrders = storeOrderRepository.findAllByCustomerOrder_Id(customerOrder.getId());

            boolean allConfirmed = allStoreOrders.stream()
                    .allMatch(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()));
            boolean allCancelled = allStoreOrders.stream()
                    .allMatch(o -> "CANCELLED".equalsIgnoreCase(o.getStatus()));

            String newStatus;
            if (allConfirmed) {
                newStatus = "CONFIRMED";
            } else if (allCancelled) {
                newStatus = "CANCELLED";
            } else {
                newStatus = "PENDING";
            }

            String current = customerOrder.getStatus() == null ? "" : customerOrder.getStatus().trim().toUpperCase();
            if (!newStatus.equals(current)) {
                customerOrder.setStatus(newStatus);
                customerOrderRepository.saveAndFlush(customerOrder);
            }
        }

        return order;
    }

}