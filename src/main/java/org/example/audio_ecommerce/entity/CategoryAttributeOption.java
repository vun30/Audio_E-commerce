package org.example.audio_ecommerce.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.UUID;
@Entity
@Table(name = "category_attribute_options")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CategoryAttributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CategoryAttribute attribute;

    @Column(nullable = false)
    private String optionValue;
}
