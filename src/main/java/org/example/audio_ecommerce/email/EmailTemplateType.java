package org.example.audio_ecommerce.email;

public enum EmailTemplateType {
    ACCOUNT_VERIFY,
    ACCOUNT_WELCOME,
    ACCOUNT_CREATED,
    KYC_SUBMITTED,   // 👈 mới: xác nhận đã nộp KYC
    KYC_APPROVED,    // duyệt
    KYC_REJECTED,    // từ chối (kèm lý do)
    ORDER_CONFIRMED,
    PASSWORD_RESET,
    RESET_PASSWORD,
    STORE_STATUS_UPDATED
}
