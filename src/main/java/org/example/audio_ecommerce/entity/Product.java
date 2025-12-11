package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 🏪 Product Entity — Đại diện cho sản phẩm âm thanh (Loa, Tai nghe, Micro, Ampli,...)
 * Được thiết kế linh hoạt, hỗ trợ nhiều loại sản phẩm và thuộc tính kỹ thuật.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "products")
public class Product {

    // =========================================================
    // 🆔 KHÓA CHÍNH & QUAN HỆ
    // =========================================================
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "product_id", columnDefinition = "CHAR(36)")
    private UUID productId;
    // 📝 NOTE: ID duy nhất của sản phẩm | Ví dụ: `550e8400-e29b-41d4-a716-446655440000`

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;
    // 📝 NOTE: Cửa hàng đăng bán | Ví dụ: `AudioPro Store (ID: 123)`

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> attributeValues;


    // =========================================================
    // 🏷️ THÔNG TIN CHUNG
    // =========================================================
    @Column(nullable = false)
    private String brandName;
    // 📝 NOTE: Tên thương hiệu | Ví dụ: `JBL`, `Sony`, `Sennheiser`, `Yamaha`

    private String name;
    // 📝 NOTE: Tên sản phẩm | Ví dụ: `JBL Charge 5`, `Sony WH-1000XM4`

    private String slug;
    // 📝 NOTE: URL thân thiện | Ví dụ: `jbl-charge-5-bluetooth-speaker`

    private String shortDescription;
    // 📝 NOTE: Mô tả ngắn | Ví dụ: `Loa Bluetooth 40W, pin 20h, chống nước IP67`

    @Lob
    private String description;
    // 📝 NOTE: Mô tả chi tiết HTML | Ví dụ: `<p>Loa JBL Charge 5 với công suất 40W...</p>`

    private String model;
    // 📝 NOTE: Mã model | Ví dụ: `JBLCHG5`, `WH1000XM4`

    private String color;
    // 📝 NOTE: Màu sắc | Ví dụ: `Black`, `Blue`, `Red`

    private String material;
    // 📝 NOTE: Chất liệu | Ví dụ: `ABS Plastic`, `Aluminum`, `Leather`

    private String dimensions;
    // 📝 NOTE: Kích thước (DxRxC) | Ví dụ: `22 x 9.6 x 9.3 cm`

    private BigDecimal weight;
    // 📝 NOTE: Trọng lượng (kg) | Ví dụ: `0.96`

     private String warrantyPeriod;
    // 📝 NOTE: Thời gian bảo hành | Ví dụ: `24 tháng`

    private String warrantyType;
    // 📝 NOTE: Loại BH | Ví dụ: `1 đổi 1`, `Sửa chữa`

    // =========================================================
    // 🧩 PHÂN LOẠI SẢN PHẨM (VARIANT)
    // =========================================================
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductVariantEntity> variants;

    // =========================================================
    // 📸 HÌNH ẢNH & VIDEO
    // =========================================================
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private List<String> images;
    // 📝 NOTE: Danh sách URL ảnh | Ví dụ: `["https://cdn.img1/jbl1.jpg", "https://cdn.img2/jbl2.jpg"]`

    private String videoUrl;
    // 📝 NOTE: URL video demo | Ví dụ: `https://youtube.com/watch?v=abc123`

    // =========================================================
    // 💰 GIÁ CƠ BẢN & TỒN KHO
    // =========================================================
    private String sku;
    // 📝 NOTE: Mã SKU duy nhất | Ví dụ: `JBL-CHG5-BLK-32`

     private String approvalReason;
    // 📝 NOTE: Lý do admin chỉnh sửa | Ví dụ: `Cập nhật giá theo thị trường`



    @Column(nullable = true)
    private BigDecimal price;
    // 📝 NOTE: Giá gốc | Ví dụ: `3500000` (3.5 triệu VND)

    private BigDecimal discountPrice;
    // 📝 NOTE: Giá giảm | Ví dụ: `2990000`

    private BigDecimal promotionPercent;
    // 📝 NOTE: % khuyến mãi | Ví dụ: `15.00` (15%)

    private BigDecimal priceAfterPromotion;
    // 📝 NOTE: Giá sau khuyến mãi | Ví dụ: `2975000`

    private BigDecimal priceBeforeVoucher;
    // 📝 NOTE: Giá trước voucher | Ví dụ: `2975000`

    private BigDecimal voucherAmount; // voucher riêng dạng code

    private BigDecimal finalPrice;
    // 📝 NOTE: Giá cuối cùng | Ví dụ: `2875000` (sau voucher)

    private BigDecimal platformFeePercent;
    // 📝 NOTE: % phí nền tảng | Ví dụ: `5.00` (5%)

    private String currency;
    // 📝 NOTE: Đơn vị tiền tệ | Ví dụ: `VND`, `USD`

    private Integer stockQuantity;
    // 📝 NOTE: Số lượng tồn | Ví dụ: `50`

    private String warehouseLocation;
    // 📝 NOTE: Vị trí kho | Ví dụ: `Kho Hà Nội - KCN Thăng Long`
    // =========================================================
// 🌍 ĐỊA CHỈ HÀNH CHÍNH (CODE TỈNH, QUẬN, XÃ)
// =========================================================
    private String provinceCode;
// 📝 NOTE: Mã tỉnh/thành phố | Ví dụ: "01" (Hà Nội), "79" (TP.HCM)

    private String districtCode;
// 📝 NOTE: Mã quận/huyện | Ví dụ: "760" (Quận 1)

    private String wardCode;
// 📝 NOTE: Mã phường/xã | Ví dụ: "26734" (Phường Bến Nghé)


    private String shippingAddress;
    // 📝 NOTE: Địa chỉ giao | Ví dụ: `123 Nguyễn Trãi, Hà Nội`
    // =========================================================
    // 🚚 VẬN CHUYỂN
    // =========================================================
    private BigDecimal shippingFee;
    // 📝 NOTE: Phí ship cơ bản | Ví dụ: `30000`

    @ElementCollection
    @CollectionTable(name = "product_shipping_methods", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "shipping_method_id")
    private List<UUID> supportedShippingMethodIds;
    // 📝 NOTE: ID phương thức ship | Ví dụ: `[UUID("111"), UUID("222")]` → GHTK, GHN

    // =========================================================
    // 🧮 MUA NHIỀU GIẢM GIÁ
    // =========================================================
    @ElementCollection
    @CollectionTable(name = "product_bulk_discounts", joinColumns = @JoinColumn(name = "product_id"))
    private List<BulkDiscount> bulkDiscounts;
    // 📝 NOTE: Bảng giá sỉ | Ví dụ: `[{from:2,to:5,unitPrice:2800000}, {from:6,to:99,unitPrice:2600000}]`

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscount {
        private Integer fromQuantity; // 📝 Ví dụ: `2`
        private Integer toQuantity; // 📝 Ví dụ: `5`
        private BigDecimal unitPrice; // 📝 Ví dụ: `2800000`
    }

    // =========================================================
    // 📊 TRẠNG THÁI & ĐÁNH GIÁ
    // =========================================================
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    // 📝 NOTE: Trạng thái | Ví dụ: `ACTIVE`, `DRAFT`, `OUT_OF_STOCK`

    private Boolean isFeatured;
    // 📝 NOTE: Sản phẩm nổi bật | Ví dụ: `true` (hiển thị trang chủ)

    private BigDecimal ratingAverage;
    // 📝 NOTE: Điểm TB | Ví dụ: `4.7`

    private Integer reviewCount;
    // 📝 NOTE: Số đánh giá | Ví dụ: `125`

    private Integer viewCount;
    // 📝 NOTE: Lượt xem | Ví dụ: `24567`

    private Integer sellCount;
    // 📝 NOTE: Lượt bán | Ví dụ: `1345`

    // =========================================================

    private LocalDateTime createdAt; // 📝 Ví dụ: 2025-01-15T10:30:00
    private LocalDateTime updatedAt; // 📝 Ví dụ: 2025-01-16T14:22:00
    private LocalDateTime lastUpdatedAt;    // thời điểm update gần nhất trước đó
    private Long lastUpdateIntervalDays;    // số ngày cách lần cập nhật trước
    private UUID createdBy; // 📝 Ví dụ: UUID("user-admin-123")
    private UUID updatedBy; // 📝 Ví dụ: UUID("user-seller-456")

    // ============= Gán thời điểm khi tạo mới =============
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;          // xem như update gần nhất là khi tạo
        this.lastUpdateIntervalDays = 0L;  // lần đầu tạo => 0 ngày
    }

    // ============= Tự tính số ngày mỗi khi update =============
    @PreUpdate
    public void onUpdate() {
        LocalDateTime now = LocalDateTime.now();

        // nếu chưa có lastUpdatedAt thì dùng createdAt làm mốc
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = this.createdAt;
        }

        // tính số ngày giữa lần update trước và hiện tại
        this.lastUpdateIntervalDays =
                ChronoUnit.DAYS.between(this.lastUpdatedAt, now);

        // cập nhật lại mốc thời gian
        this.lastUpdatedAt = this.updatedAt != null ? this.updatedAt : this.createdAt;
        this.updatedAt = now;
    }


}