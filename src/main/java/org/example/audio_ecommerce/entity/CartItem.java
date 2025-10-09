package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CartItemType;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "cart_items",
        indexes = {
                @Index(name = "idx_cartitem_cart", columnList = "cart_id"),
                @Index(name = "idx_cartitem_type_ref", columnList = "type,product_id,combo_id")
        })
public class CartItem {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name="uuid2", strategy="uuid2")
    @Column(name = "cart_item_id", columnDefinition = "CHAR(36)")
    private UUID cartItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CartItemType type;

    // Một trong hai: product hoặc combo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // nullable khi type = COMBO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private ProductCombo combo; // nullable khi type = PRODUCT

    @Column(nullable = false) private Integer quantity;

    // snapshot giá tại thời điểm thêm giỏ
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    // snapshot thông tin hiển thị nhanh (tránh N+1)
    private String nameSnapshot;
    private String imageSnapshot;

    // helper
    public UUID getReferenceId() {
        if (type == CartItemType.PRODUCT && product != null) return product.getProductId();
        if (type == CartItemType.COMBO   && combo   != null) return combo.getComboId();
        return null;
    }
}
