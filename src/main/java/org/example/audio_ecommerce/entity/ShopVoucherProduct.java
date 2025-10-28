package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shop_voucher_products")
public class ShopVoucherProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔹 Quan hệ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private ShopVoucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 🔹 Cấu hình áp dụng
    private Integer promotionStockLimit;      // Số lượng được áp voucher
    private Integer purchaseLimitPerCustomer; // Giới hạn mua / user
    private boolean active = true;
}
