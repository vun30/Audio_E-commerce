package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.response.GhnOrderResponse;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.GhnOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.GhnOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.GhnOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GhnOrderServiceImpl implements GhnOrderService {

    private final GhnOrderRepository repo;
    private final StoreOrderRepository storeOrderRepo;
    private final CustomerOrderRepository customerOrderRepo;
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

    @Override
    @Transactional
    public GhnOrder updateStatus(UUID ghnOrderId, GhnStatus status) {

        GhnOrder ghn = repo.findById(ghnOrderId)
                .orElseThrow(() -> new NoSuchElementException("GHN order not found: " + ghnOrderId));

        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        // 1) update GHN
        ghn.setStatus(status);
        GhnOrder savedGhn = repo.save(ghn);

        // 2) map GHN -> OrderStatus
        OrderStatus mappedOrderStatus = mapGhnToOrderStatus(status);

        // 3) update StoreOrder (theo storeOrderId trong GHN)
        StoreOrder so = storeOrderRepo.findById(ghn.getStoreOrderId())
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found: " + ghn.getStoreOrderId()));

        so.setStatus(mappedOrderStatus);

        // nếu giao thành công thì set deliveredAt
        if (mappedOrderStatus == OrderStatus.DELIVERY_SUCCESS
                || mappedOrderStatus == OrderStatus.COMPLETED
                || mappedOrderStatus == OrderStatus.DELIVERED_WAITING_CONFIRM) {
            so.setDeliveredAt(LocalDateTime.now());
        }

        storeOrderRepo.save(so);

        // 4) update CustomerOrder (lấy từ storeOrder)
        CustomerOrder co = so.getCustomerOrder(); // StoreOrder có customerOrder :contentReference[oaicite:2]{index=2}
        if (co != null) {
            co.setStatus(mappedOrderStatus);

            if (mappedOrderStatus == OrderStatus.DELIVERY_SUCCESS
                    || mappedOrderStatus == OrderStatus.COMPLETED
                    || mappedOrderStatus == OrderStatus.DELIVERED_WAITING_CONFIRM) {
                co.setDeliveredAt(LocalDateTime.now());
            }

            customerOrderRepo.save(co);
        }

        return savedGhn;
    }

    private OrderStatus mapGhnToOrderStatus(GhnStatus s) {
        if (s == null) return OrderStatus.EXCEPTION;

        return switch (s) {
            case GHN_CREATED -> OrderStatus.GHN_CREATED;

            // ==== chuẩn bị lấy hàng / kho ====
            case READY_TO_PICK, READY_PICKUP, PICKING, PICKED, STORING, SORTING, TRANSPORTING
                    -> OrderStatus.READY_FOR_PICKUP;

            // ==== đang giao ====
            case DELIVERING, MONEY_COLLECT_DELIVERING, ON_DELIVERY
                    -> OrderStatus.OUT_FOR_DELIVERY;

            // ==== giao thành công ====
            case DELIVERED
                    -> OrderStatus.DELIVERY_SUCCESS;

            // ==== giao thất bại ====
            case DELIVERY_FAIL
                    -> OrderStatus.DELIVERY_FAIL;

            // ==== hủy ====
            case CANCEL, CANCELED
                    -> OrderStatus.CANCELLED;

            // ==== hoàn hàng / trả ====
            case WAITING_TO_RETURN, RETURN_TRANSPORTING, RETURN_SORTING, RETURNING, RETURN_FAIL, RETURN
                    -> OrderStatus.RETURNING;

            case RETURNED
                    -> OrderStatus.RETURNED;

            // ==== lỗi đặc biệt ====
            case EXCEPTION, DAMAGE, LOST
                    -> OrderStatus.EXCEPTION;
            default -> OrderStatus.EXCEPTION;
        };
    }

}
