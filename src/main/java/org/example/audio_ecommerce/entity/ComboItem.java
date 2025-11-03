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

    // để sau FE có thể nhập mỗi sp số lượng khác nhau trong combo
    @Column(nullable = false)
    private Integer quantity = 1;
}
