package org.example.audio_ecommerce.entity.Enum;

public enum ReturnStatus {
    PENDING,          // khách gửi yêu cầu
    APPROVED,         // shop duyệt → chờ khách gửi hàng
    SHIPPING,         // GHN đang vận chuyển
    RECEIVED,         // shop xác nhận đã nhận đúng hàng
    DISPUTE,          // đang khiếu nại
    REFUNDED,         // đã hoàn tiền
    REJECTED,         // từ chối hoàn
    AUTO_REFUNDED,     // hệ thống auto hoàn (shop im lặng / complaint)
    RETURN_DONE,      // hoàn tất quy trình trả hàng
    CANCELED,          // khách hủy yêu cầu trả hàng
    DISPUTE_ESCALATED,  // khiếu nại đã được đưa lên sàn xử lý
    DISPUTE_RESOLVED_SHOP,   // khiếu nại đã được giải quyết có lợi cho shop
    DISPUTE_RESOLVED_CUSTOMER, // khiếu nại đã được giải quyết có lợi cho khách hàng
    CANCELLED

}
