package org.example.audio_ecommerce.entity.Enum;

public enum StoreWalletTransactionType {
    DEPOSIT,                // 💰 Cộng tiền khi đơn hàng hoàn tất
    PENDING_HOLD,           // ⏳ Tiền giữ ở pending khi thanh toán
    RELEASE_PENDING,        // 🔁 Chuyển tiền từ pending sang có thể rút
    WITHDRAW,               // 💸 Rút tiền
    REFUND,                 // 🔄 Hoàn tiền cho khách
    ADJUSTMENT              // ⚙️ Điều chỉnh thủ công (admin)
}
