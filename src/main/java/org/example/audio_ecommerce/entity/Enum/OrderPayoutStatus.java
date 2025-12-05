package org.example.audio_ecommerce.entity.Enum;

public enum OrderPayoutStatus {
     NOT_ELIGIBLE,     // chưa giao đủ 7 ngày
    HOLD_DISPUTE,     // đang bị kiện / return → tạm khóa
    ELIGIBLE,         // đủ điều kiện payout
    INCLUDED,         // đã đưa vào bill
    PAID_OUT,         // đã chuyển tiền
    DISPUTE_CLEARED   // dispute đã clear - quay lại luồng payout
}
