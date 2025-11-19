package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.CustomerOrderCancellationRequest;
import org.example.audio_ecommerce.entity.Enum.CancellationReason;
import org.example.audio_ecommerce.entity.StoreOrderCancellationRequest;

import java.util.List;
import java.util.UUID;

public interface OrderCancellationService {
    BaseResponse<Void> customerCancelWholeOrderIfPending(UUID customerId, UUID customerOrderId,
                                                         CancellationReason reason, String note);

    // KH yêu cầu hủy dựa vào customerOrderId (map sang store-order duy nhất)
    BaseResponse<Void> customerRequestCancelStoreOrderByCustomerOrderId(UUID customerId, UUID customerOrderId,
                                                                        CancellationReason reason, String note);

    BaseResponse<Void> shopApproveCancel(UUID storeId, UUID storeOrderId);
    BaseResponse<Void> shopRejectCancel(UUID storeId, UUID storeOrderId, String note);


    List<StoreOrderCancellationRequest> getCustomerCancellationRequests(
            UUID customerId, UUID customerOrderId
    );
    List<StoreOrderCancellationRequest> getStoreCancellationRequests(
            UUID storeId, UUID storeOrderId
    );

    // ✅ NEW: customer xem tất cả cancel của mình (bảng customer_order_cancellation)
    List<CustomerOrderCancellationRequest> getAllCustomerOrderCancellations(UUID customerId);

    // ✅ NEW: store xem tất cả request huỷ của toàn bộ store-order
    List<StoreOrderCancellationRequest> getAllStoreCancellationRequests(UUID storeId);
}


