package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String optionName;     // Color, Size,...

    @Column(nullable = false)
    private String optionValue;    // Black, M,...

    @Column(nullable = false)
    private BigDecimal variantPrice;

    @Column(nullable = false)
    private Integer variantStock;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String variantUrl;

    @Column(unique = false)
    private String variantSku;     // SKU riêng của biến thể

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
