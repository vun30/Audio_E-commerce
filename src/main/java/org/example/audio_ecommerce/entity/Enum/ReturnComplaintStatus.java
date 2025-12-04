package org.example.audio_ecommerce.entity.Enum;

public enum ReturnComplaintStatus {
    OPEN,           // mới tạo
    IN_REVIEW,      // admin đang xử lý
    RESOLVED,       // đã xử lý (auto refund / manual)
    REJECTED        // từ chối khiếu nại
}
