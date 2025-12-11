package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "product_attribute_values")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductAttributeValue {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CategoryAttribute attribute;

    @Column(columnDefinition = "LONGTEXT")
    private String value;
}
