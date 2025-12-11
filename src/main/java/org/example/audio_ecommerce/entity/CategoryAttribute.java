package org.example.audio_ecommerce.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "category_attributes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryAttribute {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "attribute_id", columnDefinition = "CHAR(36)")
    private UUID attributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String attributeName;
    // Ví dụ: "frequencyResponse", "driverSize", "powerHandling"

    @Column(nullable = false)
    private String attributeLabel;
    // Ví dụ: "Dải tần", "Kích thước Driver", "Công suất"

    @Column(nullable = false)
    private String dataType;
    // STRING / NUMBER / BOOLEAN / ENUM / JSON
}

