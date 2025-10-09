package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CartItemType;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name="uuid2", strategy="uuid2")
    @Column(name = "cart_item_id", columnDefinition = "CHAR(36)")
    private UUID cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartItemType type; // PRODUCT hoặc COMBO

    // Chỉ 1 trong 2 field dưới có giá trị
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private ProductCombo combo; // nullable

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, scale = 2, precision = 18)
    private BigDecimal unitPrice;

    @Column(nullable = false, scale = 2, precision = 18)
    private BigDecimal totalPrice; // = unitPrice * quantity
}