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
@Table(name = "combo_items")
public class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "combo_item_id", columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private ProductCombo combo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ============================
    // 🆕 THÔNG TIN BIẾN THỂ
    // ============================

    @Column(name = "variant_id", columnDefinition = "CHAR(36)")
    private UUID variantId;

    @Column(nullable = false)
    private String optionName;     // Color, Size...

    @Column(nullable = false)
    private String optionValue;    // Black, M...

    @Column(nullable = false)
    private BigDecimal variantPrice;

    @Column(nullable = false)
    private Integer variantStock;

    @Column(nullable = false)
    private String variantUrl;

    @Column(unique = false)
    private String variantSku;

    // ============================
    // Số lượng của product trong combo
    // ============================
    @Column(nullable = false)
    private Integer quantity = 1;
}
