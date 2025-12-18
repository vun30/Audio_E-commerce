package org.example.audio_ecommerce.entity.Enum;

public enum StoreWalletTransactionType {

    /**
     * 💰 TIỀN BÁN HÀNG
     * Platform ghi nhận doanh thu cho store
     * (tiền từ đơn hàng hoàn tất, payout vào ví default)
     */
    DEPOSIT,

    /**
     * ⏳ GIỮ TIỀN TẠM THỜI
     * Tiền bị hold khi:
     * - đơn chưa hoàn tất
     * - chờ xác nhận / đối soát
     */
    PENDING_HOLD,

    /**
     * 🔁 GIẢI PHÓNG TIỀN HOLD
     * Chuyển tiền từ pending → default
     */
    RELEASE_PENDING,

    /**
     * 💸 RÚT TIỀN
     * Store rút tiền từ ví default về ngân hàng
     */
    WITHDRAW,

    /**
     * 🔄 HOÀN TIỀN CHO KHÁCH
     * Hoàn tiền thông thường (customer cancel / lỗi hệ thống)
     */
    REFUND,

    /**
     * ⚙️ ĐIỀU CHỈNH THỦ CÔNG
     * Admin điều chỉnh số dư (cộng/trừ)
     */
    ADJUSTMENT,

    /**
     * 📦 HOÀN TIỀN DO TRẢ HÀNG
     * Hoàn tiền phát sinh từ return / hoàn đơn
     */
    REFUND_RETURN,

    /**
     * ⚠️ ÉP HOÀN TIỀN
     * Hoàn tiền cưỡng chế bởi admin / hệ thống
     */
    REFUND_FORCE,

    /**
     * 💳 NẠP TIỀN VÀO VÍ
     * Store tự nạp tiền (thường dùng cho:
     * - bổ sung ký quỹ
     * - đảm bảo hạn mức nợ)
     */
    TOPUP,

    /**
     * ❌ THANH TOÁN NỢ
     * Trừ tiền từ ví default để thanh toán các khoản nợ
     */
    DEBT_PAYMENT,

    /**
     * 🔀 CHUYỂN TIỀN GIỮA CÁC VÍ
     * Chuyển tiền giữa các ví của cùng 1 store
     * (ví dụ: từ ví default sang ví deposit ký quỹ)
     */
    TRANSFER_TO_DEPOSIT,

//    **
//     * 🔀 CHUYỂN TIỀN GIỮA CÁC VÍ
//     * Chuyển tiền giữa các ví của cùng 1 store
//     * (ví dụ: từ ví deposit ký quỹ sang ví default)
//     */

    TRANSFER_DEPOSIT_TO_DEFAULT
}