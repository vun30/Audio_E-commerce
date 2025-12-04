package org.example.audio_ecommerce.entity.Enum;

public enum ReturnStatus {
    PENDING,          // khách gửi yêu cầu
    APPROVED,         // shop duyệt → chờ khách gửi hàng
    SHIPPING,         // GHN đang vận chuyển
    RECEIVED,         // shop xác nhận đã nhận đúng hàng
    DISPUTE,          // đang khiếu nại
    REFUNDED,         // đã hoàn tiền
    REJECTED,         // từ chối hoàn
    AUTO_REFUNDED     // hệ thống auto hoàn (shop im lặng / complaint)
}
