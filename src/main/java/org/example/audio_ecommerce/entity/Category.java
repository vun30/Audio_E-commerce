package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "category_id", columnDefinition = "CHAR(36)")
    private UUID categoryId;

    @Column(nullable = false, unique = true, length = 255)
    private String name; // 🔹 Tên danh mục (VD: Loa, Micro, DAC, Mixer, Amp,...)

    @Column(length = 255)
    private String slug; // 🔹 Slug SEO

    @Column(columnDefinition = "TEXT")
    private String description; // 🔹 Mô tả danh mục

    private String iconUrl; // 🔹 Icon đại diện danh mục

    private Integer sortOrder; // 🔹 Thứ tự sắp xếp hiển thị
}
