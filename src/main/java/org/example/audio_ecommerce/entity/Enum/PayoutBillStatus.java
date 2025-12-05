package org.example.audio_ecommerce.entity.Enum;

public enum PayoutBillStatus {
    PENDING,     // Bill đã tạo, chờ admin đối soát
    REVIEW,      // Admin kiểm tra thủ công
    PAID,        // Đã chuyển khoản
    CANCELED     // Hủy bill payout
}
