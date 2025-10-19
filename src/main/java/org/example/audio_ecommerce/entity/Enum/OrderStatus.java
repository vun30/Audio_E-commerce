package org.example.audio_ecommerce.entity.Enum;

public enum OrderStatus {
    UNPAID,             // Chờ thanh toán (online)
    CONFIRMED,          // Đã xác nhận (đã thanh toán / COD)
    AWAITING_SHIPMENT,  // Chờ lấy hàng (đã thanh toán / COD)
    SHIPPING,           // Đang giao hàng
    COMPLETED,          // Đã giao hàng / Hoàn tất
    CANCELLED,          // Đã hủy
    RETURN_REQUESTED,   // Yêu cầu trả hàng / hoàn tiền
    RETURNED,           // Đã trả hàng / hoàn tiền xong
    PENDING             // Tạm thêm nếu bạn vẫn cần cho store chưa xử lý
}
