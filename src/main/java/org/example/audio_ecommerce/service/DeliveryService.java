package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.DeliveryAssignmentResponse;
import org.example.audio_ecommerce.entity.DeliveryAssignment;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface DeliveryService {
    // 1) phân công nhân viên
    void assignDeliveryStaff(UUID storeId, UUID storeOrderId, UUID deliveryStaffId, UUID preparedByStaffId, String note);

    // 2) kho xác nhận "ready" (chuẩn bị xong)
    void markReadyForPickup(UUID storeId, UUID storeOrderId);

    // 3) shipper bấm "nhận hàng rời kho"
    void markOutForDelivery(UUID storeId, UUID storeOrderId);

    // 4) shipper bấm "đã giao đến địa chỉ" (chờ xác nhận)
    void markDeliveredWaitingConfirm(UUID storeId, UUID storeOrderId);

    // 5) shipper chụp ảnh/xác nhận lắp đặt → hoàn tất
    void confirmDeliverySuccess(UUID storeId, UUID storeOrderId, String photoUrl, boolean installed, String note);

    // 6) khách từ chối nhận
    void markDeliveryDenied(UUID storeId, UUID storeOrderId, String reason);

    // 7) shipper gửi location định kỳ
    void pushLocation(UUID storeId, UUID storeOrderId, Double lat, Double lng, Double speed, String addressText);

    List<DeliveryAssignmentResponse> listAssignments(UUID storeId, OrderStatus status);
    Page<DeliveryAssignmentResponse> pageAssignments(UUID storeId, OrderStatus status, int page, int size, String sort);
    DeliveryAssignmentResponse getAssignment(UUID storeId, UUID assignmentId);
    // 11) tiện ích: lấy StoreOrder sau cập nhật
    StoreOrder getStoreOrderEntity(UUID storeOrderId);
}
