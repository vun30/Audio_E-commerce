package org.example.audio_ecommerce.entity.Enum;

public enum WalletTransactionType {
    // ===== Nạp / rút cơ bản =====
    DEPOSIT,
    WITHDRAW,
    TOPUP,

    // ===== Thanh toán / hoàn tiền =====
    PAYMENT,
    REFUND,

    // ===== Phí hệ thống =====
    SYSTEM_FEE,

    // ===== QR / Wallet internal =====
    QR,
    WALLET,

    // ===== Refund / Return flow (shop) =====
    RETURN_REFUND_SHOP_DEBIT,          // trừ tiền shop khi refund
    FORCE_RETURN_REFUND_SHOP,          // system cưỡng chế refund shop

    // ===== Refund / Return flow (customer) =====
    RETURN_REFUND_CUSTOMER_CREDIT,     // cộng tiền customer khi refund
    FORCE_RETURN_REFUND_CUSTOMER,      // system cưỡng chế refund customer

    // ===== Withdraw flow (customer / store) =====
    WITHDRAW_REQUEST,                  // tạo yêu cầu rút (PENDING)
    WITHDRAW_RELEASE,                  // reject/cancel → trả tiền về balance
    WITHDRAW_PAYOUT                    // payout thành công
}
