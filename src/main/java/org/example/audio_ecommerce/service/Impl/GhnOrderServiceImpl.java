package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.response.GhnOrderResponse;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.entity.GhnOrder;
import org.example.audio_ecommerce.repository.GhnOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.GhnOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GhnOrderServiceImpl implements GhnOrderService {

    private final GhnOrderRepository repo;
    private final StoreOrderRepository storeOrderRepo;

    @Transactional
    @Override
    public GhnOrderResponse create(CreateGhnOrderRequest req) {
        GhnOrder entity = GhnOrder.builder()
                .storeOrderId(req.getStoreOrderId())
                .storeId(req.getStoreId())
                .orderGhn(req.getOrderGhn())
                .totalFee(req.getTotalFee())
                .expectedDeliveryTime(req.getExpectedDeliveryTime())
                .status(GhnStatus.READY_PICKUP)
                .build();


        entity = repo.save(entity);

storeOrderRepo.findById(req.getStoreOrderId()).ifPresent(storeOrder -> {
            storeOrder.setShippingFeeReal(req.getTotalFee() != null ? req.getTotalFee() : BigDecimal.ZERO);
storeOrderRepo.save(storeOrder);
        });
        return toResp(entity);
    }

    @Override
    public GhnOrderResponse getByStoreOrderId(UUID storeOrderId) {
        GhnOrder e = repo.findByStoreOrderId(storeOrderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("GHN order not found"));
        return toResp(e);
    }

    private GhnOrderResponse toResp(GhnOrder e) {
        return GhnOrderResponse.builder()
                .id(e.getId())
                .storeOrderId(e.getStoreOrderId())
                .storeId(e.getStoreId())
                .orderGhn(e.getOrderGhn())
                .totalFee(e.getTotalFee())
                .expectedDeliveryTime(e.getExpectedDeliveryTime())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
