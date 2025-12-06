package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ComboCreatorType;
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

    // 🔗 Nếu là shop tạo combo -> có storeId, cus tạo combo -> null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // 💡 Category mặc định COMBO -> không cần lưu UUID category
    // FE BE trả response gửi text "COMBO" cố định.

    // 📦 các product con trong combo
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ComboItem> items;

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
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private List<String> images;

    private String videoUrl;

    // ⚖️ logistics info
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    private String provinceCode;
    private String districtCode;
    private String wardCode;

    private ComboCreatorType creatorType; // SHOP_CREATE or CUSTOMER_CREATE
    private UUID creatorId; // id của shop or customer

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private UUID updatedBy;
}
