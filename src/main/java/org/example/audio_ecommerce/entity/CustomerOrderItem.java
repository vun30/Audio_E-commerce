package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_order_item")
public class CustomerOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @Column(nullable = false)
    private String type; // PRODUCT or COMBO

    @Column(nullable = false)
    private UUID refId; // productId or comboId

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    // ==== NEW: thông tin variant ====
    @Column(name = "variant_id")
    private UUID variantId;                 // có thể null

    @Column(name = "variant_option_name", length = 100)
    private String variantOptionName;       // ví dụ: "Color"

    @Column(name = "variant_option_value", length = 255)
    private String variantOptionValue;      // ví dụ: "Black"

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal lineTotal;

    @Column(nullable = false)
    private UUID storeId;
}
