package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.CancellationReason;

import java.util.UUID;

public interface OrderCancellationService {
    BaseResponse<Void> customerCancelWholeOrderIfPending(UUID customerId, UUID customerOrderId,
                                                         CancellationReason reason, String note);

    // KH yêu cầu hủy dựa vào customerOrderId (map sang store-order duy nhất)
    BaseResponse<Void> customerRequestCancelStoreOrderByCustomerOrderId(UUID customerId, UUID customerOrderId,
                                                                        CancellationReason reason, String note);

    BaseResponse<Void> shopApproveCancel(UUID storeId, UUID storeOrderId);
    BaseResponse<Void> shopRejectCancel(UUID storeId, UUID storeOrderId, String note);
}


