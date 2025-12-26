package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreRevenueDetailResponse;
import org.example.audio_ecommerce.dto.response.StoreTotalRevenueResponse;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.StoreRevenueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreRevenueServiceImpl implements StoreRevenueService {

    private final StoreOrderRepository storeOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreTotalRevenueResponse getStoreTotalRevenue(UUID storeId) {
        // Find all delivered orders for the store
        List<StoreOrder> deliveredOrders = storeOrderRepository.findDeliveredOrdersByStoreId(storeId);

        // Calculate total platform fee revenue
        BigDecimal totalPlatformFeeRevenue = deliveredOrders.stream()
                .map(StoreOrder::getPlatformFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Convert to detail responses
        List<StoreRevenueDetailResponse> orderDetails = deliveredOrders.stream()
                .map(this::mapToRevenueDetailResponse)
                .collect(Collectors.toList());

        return StoreTotalRevenueResponse.builder()
                .storeId(storeId)
                .totalPlatformFeeRevenue(totalPlatformFeeRevenue)
                .totalDeliveredOrders((long) deliveredOrders.size())
                .deliveredOrders(orderDetails)
                .build();
    }

    private StoreRevenueDetailResponse mapToRevenueDetailResponse(StoreOrder order) {
        return StoreRevenueDetailResponse.builder()
                .storeOrderId(order.getId())
                .orderCode(order.getOrderCode())
                .grandTotal(order.getGrandTotal())
                .platformFeePercentage(order.getPlatformFeePercentage())
                .platformFeeAmount(order.getPlatformFeeAmount())
                .deliveredAt(order.getDeliveredAt())
                .status(order.getStatus().name())
                .build();
    }
}