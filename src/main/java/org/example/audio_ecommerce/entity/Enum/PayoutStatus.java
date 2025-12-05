package org.example.audio_ecommerce.entity.Enum;

public enum PayoutStatus {
    PENDING,       // Đang chờ duyệt
    PROCESSING,    // Đang chuyển khoản
    PAID,          // Đã trả tiền
    FAILED,        // Chuyển khoản thất bại
    CANCELLED      // Sàn hủy bill
}
