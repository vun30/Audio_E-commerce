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
@Table(name = "shop_voucher_products")
public class ShopVoucherProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ========== 🔹 Quan hệ ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private ShopVoucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ========== 🔹 Thông tin giảm giá ==========
    private BigDecimal originalPrice;      // Giá gốc của sản phẩm
    private BigDecimal discountedPrice;    // Giá sau giảm
    private Integer discountPercent;       // % giảm riêng (nếu có)
    private BigDecimal discountAmount;     // Số tiền giảm (nếu có)

    // ========== 🔹 Hạn mức sản phẩm ==========
    private Integer stock;                 // Tổng kho của sản phẩm
    private Integer promotionStockLimit;   // Số lượng áp dụng khuyến mãi
    private Integer purchaseLimitPerCustomer; // Mỗi KH được mua tối đa bao nhiêu

    // ========== 🔹 Trạng thái ==========
    private boolean isActive = true;       // Có đang áp dụng khuyến mãi không
}
