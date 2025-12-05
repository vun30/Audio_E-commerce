package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "return_shipping_fees")
public class ReturnShippingFee extends BaseEntity {

    // ===== LIÊN KẾT ĐẾN RETURN REQUEST =====
    @Column(name = "return_request_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID returnRequestId;

    // ===== LIÊN KẾT ĐẾN STORE (TỰ ĐỘNG LẤY TỪ RETURN REQUEST) =====
    @Column(name = "store_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID storeId;  // ✅ ID chủ shop của đơn return

    // ===== GHN ORDER INFO =====
    @Column(name = "ghn_order_code", length = 64)
    private String ghnOrderCode;

    // ===== PHÍ SHIP =====
    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee;

    // ===== AI CHỊU PHÍ SHIP? =====
    // "CUSTOMER" = khách chịu / "SHOP" = shop chịu
    @Column(name = "payer", nullable = false, length = 20)
    private String payer;

    // ===== TÍNH TOÁN CHO SHOP =====
    // Số tiền thực tế sẽ trừ vào shop khi đối soát
    @Column(name = "charged_to_shop", precision = 18, scale = 2)
    private BigDecimal chargedToShop;

    // ===== FAULT TYPE =====
    // flag cho case shop fault
    @Column(name = "shop_fault")
    private Boolean shopFault;

    @Column(name = "paid_by_shop")
    private Boolean paidByShop = false;

    // ===== STATUS =====
    @Builder.Default
    @Column(name = "picked", nullable = false)
    private boolean picked = false;
}
