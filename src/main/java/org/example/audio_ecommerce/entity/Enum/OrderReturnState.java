package org.example.audio_ecommerce.entity.Enum;

public enum OrderReturnState {
    NONE,           // mặc định
    REQUESTED,      // customer đã tạo return request
    PROCESSING,     // shop/admin đang xử lý
    CANCELLED,      // customer huỷ
    DONE            // đã refund / kết thúc
}
