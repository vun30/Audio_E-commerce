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
    PENDING,             // Tạm thêm nếu bạn vẫn cần cho store chưa xử lý
    // ==== NEW cho luồng nội bộ ====
    READY_FOR_PICKUP,          // nhân viên kho/chuẩn bị hàng xong (chờ nhân viên giao nhận)
    OUT_FOR_DELIVERY,          // đang giao (đẩy geo-location định kỳ)
    DELIVERED_WAITING_CONFIRM, // giao tới đúng địa chỉ, chờ xác nhận/biên bản
    DELIVERY_SUCCESS,          // giao thành công (chụp hình hoặc tick “đã lắp đặt”)
    DELIVERY_DENIED,            // khách từ chối nhận (order deny receive)
    READY_FOR_DELIVERY,          // hàng đã sẵn sàng giao (chờ nhân viên giao nhận đến lấy)
    DELIVERY_FAIL,
    EXCEPTION,

       // KH đã gửi yêu cầu hoàn
    RETURNING,          // đang trên đường trả


//    HOLD_FOR_RETURN,               // Đang giữ tiền chờ shop xử lý
//    RETURN_APPROVED,               // Shop đồng ý trả hàng
//    RETURN_REJECTED_BY_SHOP,       // Shop từ chối → chuyển tranh chấp
//    AUTO_APPROVED,                 // Shop không phản hồi → tự động duyệt
//    DISPUTE_PENDING,               // Đang tranh chấp (chờ sàn xử lý)
//    DISPUTE_RESOLVED,              // Sàn đã xử lý xong tranh chấp
//    REFUNDED,                      // Đã hoàn tiền cho khách
//    RETURN_REJECTED_FINAL          // Từ chối cuối cùng (sàn từ chối)



}
