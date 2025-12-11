package org.example.audio_ecommerce.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "category_attributes")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attribute_id", columnDefinition = "CHAR(36)")
    private UUID attributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String attributeName;

    @Column(nullable = false)
    private String attributeLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryAttributeDataType dataType;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryAttributeOption> options = new ArrayList<>();
}


