package org.example.audio_ecommerce.entity.Enum;

public enum ItemRefundStatus {
     NONE,               // chưa refund
    REQUESTED,          // khách yêu cầu refund item này
    APPROVED,           // shop/sàn duyệt yêu cầu
    RETURNING,          // GHN đang chuyển hàng về shop
    RETURN_SUCCESS,     // shop nhận hàng → refund thành công
    RETURN_FAILED       // khách không gửi / hết hạn / gửi sai
}
