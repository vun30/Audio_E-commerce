package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_combos")
public class ProductCombo {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "combo_id", columnDefinition = "CHAR(36)")
    private UUID comboId;

    // 🔗 Combo chính là 1 product luôn
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product comboProduct; // product đại diện combo (để bán, thêm giỏ hàng, SEO...)

    // 📦 Danh sách sản phẩm thuộc combo
    @ManyToMany
    @JoinTable(
            name = "combo_items",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> includedProducts;

    // 🏬 🔗 Cửa hàng sở hữu combo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 🖼️ Ảnh đại diện combo
    private String comboImageUrl;

    // 📂 Danh mục chính của combo (VD: "Combo nghe nhạc", "Combo thu âm", ...)
    private String categoryName;
    private String categoryIconUrl;

    // 📝 Mô tả combo
    @Column(columnDefinition = "TEXT")
    private String comboDescription;

    // 💰 Giá combo (giá bán cuối cùng)
    private BigDecimal comboPrice;

    // 📊 Giá tổng sản phẩm nếu mua lẻ (để hiển thị % giảm giá)
    private BigDecimal originalTotalPrice;

    // ✅ Có đang bán combo này hay không
    private Boolean isActive;
}
