package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_combos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductCombo {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "combo_id", columnDefinition = "CHAR(36)")
    private UUID comboId;

    // 🔗 Cửa hàng tạo combo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 📂 Danh mục
    @Column(name = "category_id", columnDefinition = "CHAR(36)")
    private UUID categoryId;

    // 📦 Danh sách sản phẩm trong combo
    @ManyToMany
    @JoinTable(
            name = "combo_items",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> includedProducts;

    // 🏷️ Thông tin cơ bản
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 📸 Media
    @ElementCollection
    @CollectionTable(name = "combo_images", joinColumns = @JoinColumn(name = "combo_id"))
    @Column(name = "image_url")
    private List<String> images;

    private String videoUrl;

    // ⚖️ Thông số kỹ thuật / giao hàng
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo
    @Column(nullable = false)
    private BigDecimal comboPrice;

    private BigDecimal originalTotalPrice;

    // 📊 Trạng thái
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private UUID updatedBy;
}
