package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(generator = "uuid2") // Sinh UUID tự động
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "product_id", columnDefinition = "CHAR(36)")
    private UUID productId; // 🔹 Mã sản phẩm (PK)

    // =====================
    // 🔹 Liên kết với Store thay vì chỉ lưu storeId
    // =====================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store; // 🔹 Cửa hàng sở hữu (FK)

    @Column(name = "category_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID categoryId; // 🔹 Danh mục sản phẩm (loa, DAC, micro…)

    @Column(name = "brand_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID brandId; // 🔹 Thương hiệu (FK)

    private String name; // 🔹 Tên sản phẩm

    @Column(unique = true)
    private String slug; // 🔹 Đường dẫn SEO thân thiện

    private String shortDescription; // 🔹 Mô tả ngắn gọn

    @Lob
    private String description; // 🔹 Mô tả chi tiết

    // =====================
    // 🔹 Danh sách ảnh
    // =====================
    @ElementCollection
    @CollectionTable(
        name = "product_images",
        joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url")
    private List<String> images; // 🔹 Danh sách URL ảnh

    private String videoUrl; // 🔹 Link video giới thiệu
    private String model; // 🔹 Mã model
    private String color; // 🔹 Màu sắc
    private String material; // 🔹 Chất liệu
    private String dimensions; // 🔹 Kích thước
    private BigDecimal weight; // 🔹 Trọng lượng

    private String powerOutput; // 🔹 Công suất đầu ra
    private String connectorType; // 🔹 Loại cổng kết nối
    private String compatibility; // 🔹 Tương thích thiết bị

    @Column(unique = true, nullable = false)
    private String sku; // 🔹 Mã SKU
    private BigDecimal price; // 🔹 Giá gốc
    private BigDecimal discountPrice; // 🔹 Giá sau giảm
    private String currency; // 🔹 Loại tiền tệ (VND, USD…)
    private Integer stockQuantity; // 🔹 Số lượng tồn kho
    private String warehouseLocation; // 🔹 Vị trí kho

    // 🔹 Địa chỉ shop đưa hàng cho shipper
    @Column(length = 500)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    private ProductStatus status; // 🔹 Trạng thái

    private Boolean isFeatured; // 🔹 Nổi bật
    private BigDecimal ratingAverage; // 🔹 Điểm trung bình
    private Integer reviewCount; // 🔹 Số lượt đánh giá
    private Integer viewCount; // 🔹 Lượt xem

    private LocalDateTime createdAt; // 🔹 Ngày tạo
    private LocalDateTime updatedAt; // 🔹 Ngày cập nhật

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private UUID createdBy; // 🔹 Người tạo

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private UUID updatedBy; // 🔹 Người cập nhật

    // =========================
    // 🔊 Thuộc tính kỹ thuật riêng cho Loa
    // =========================
    private String driverConfiguration;   // 🔹 Cấu hình driver
    private String driverSize;            // 🔹 Kích thước driver
    private String frequencyResponse;     // 🔹 Dải tần
    private String sensitivity;           // 🔹 Độ nhạy
    private String impedance;             // 🔹 Trở kháng
    private String powerHandling;         // 🔹 Công suất chịu tải
    private String enclosureType;         // 🔹 Kiểu thùng loa
    private String coveragePattern;       // 🔹 Góc phủ âm
    private String crossoverFrequency;    // 🔹 Tần số cắt
    private String placementType;         // 🔹 Kiểu đặt
}
