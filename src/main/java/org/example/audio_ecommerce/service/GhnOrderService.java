package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.response.GhnOrderResponse;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.entity.GhnOrder;

import java.util.UUID;

public interface GhnOrderService {

    /**
     * Tạo mới GHN order từ request đầy đủ (storeId, storeOrderId, orderGhn, totalFee, expectedDeliveryTime, status)
     */
    GhnOrderResponse create(CreateGhnOrderRequest req);

    /**
     * Lấy thông tin GHN order theo storeOrderId
     */
    GhnOrderResponse getByStoreOrderId(UUID storeOrderId);

    GhnOrder updateStatus(UUID ghnOrderId, GhnStatus status);
}
