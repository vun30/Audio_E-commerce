package org.example.audio_ecommerce.entity.Enum;

import lombok.Getter;

@Getter
public enum PolicyCategoryType {
    PRIVACY_POLICY("Chính sách bảo mật"),
    TERMS_OF_USE("Điều khoản sử dụng"),
    PROMOTIONS_OFFERS("Khuyến mãi & Ưu đãi"),
    PAYMENT("Thanh toán"),
    ORDER_SHIPPING("Đơn hàng & Vận chuyển"),
    RETURN_REFUND("Trả hàng & Hoàn tiền");

    private final String displayName;

    PolicyCategoryType(String displayName) {
        this.displayName = displayName;
    }
}


