package org.example.audio_ecommerce.entity.Enum;

public enum TransactionType {
    HOLD,        // Giữ tiền sau khi khách thanh toán
    RELEASE,     // Giải phóng tiền về shop
    REFUND,      // Hoàn tiền về customer
    TRANSFER,    // Chuyển giữa ví
    WITHDRAW,    // Shop rút tiền
    DEPOSIT,    // Nạp tiền vào ví (nếu hệ thống có)
    INITIALIZE,  //Giao dịch mặc định khi khởi tạo ví
    PAYOUT_STORE,
    PLATFORM_FEE,
    SHIPPING_FEE_ADJUST,
    REFUND_CUSTOMER_RETURN
}