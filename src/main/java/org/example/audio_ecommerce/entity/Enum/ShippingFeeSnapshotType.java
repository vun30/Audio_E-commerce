package org.example.audio_ecommerce.entity.Enum;

/**
 * Loại snapshot phí ship
 */
public enum ShippingFeeSnapshotType {
    /**
     * Phí ước tính lúc checkout (từ GHN Fee API)
     */
    CHECKOUT_ESTIMATE,

    /**
     * Phí thực tế khi tạo đơn GHN (từ GHN Create Order API)
     */
    GHN_ACTUAL,

    /**
     * Phí cuối cùng sau settlement (có thể điều chỉnh)
     */
    SETTLEMENT_FINAL,

    /**
     * Điều chỉnh thủ công bởi admin
     */
    ADMIN_ADJUSTMENT
}

