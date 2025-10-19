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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    // 📝 NOTE: Danh mục sản phẩm | Ví dụ: `Loa Bluetooth (ID: 1)`

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

    // =========================================================
    // 🧩 PHÂN LOẠI SẢN PHẨM (VARIANT)
    // =========================================================
    @ElementCollection
    @CollectionTable(name = "product_variants", joinColumns = @JoinColumn(name = "product_id"))
    private List<ProductVariant> variants;
    // 📝 NOTE: Biến thể sản phẩm | Ví dụ: `[{optionName:"Color", optionValue:"Black"}, {optionName:"Capacity", optionValue:"32GB"}]`

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariant {
        private String optionName; // 📝 Ví dụ: `Color`, `Size`, `Capacity`
        private String optionValue; // 📝 Ví dụ: `Black`, `M`, `32GB`
    }

    // =========================================================
    // 📸 HÌNH ẢNH & VIDEO
    // =========================================================
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images;
    // 📝 NOTE: Danh sách URL ảnh | Ví dụ: `["https://cdn.img1/jbl1.jpg", "https://cdn.img2/jbl2.jpg"]`

    private String videoUrl;
    // 📝 NOTE: URL video demo | Ví dụ: `https://youtube.com/watch?v=abc123`

    // =========================================================
    // 💰 GIÁ CƠ BẢN & TỒN KHO
    // =========================================================
    private String sku;
    // 📝 NOTE: Mã SKU duy nhất | Ví dụ: `JBL-CHG5-BLK-32`

    @Column(nullable = false)
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

    // =========================================================
    // 🔊 THUỘC TÍNH CHUNG (CHO MỌI THIẾT BỊ)
    // =========================================================
    private String frequencyResponse;
    // 📝 NOTE: Dải tần (Hz) | Ví dụ: `60Hz - 20kHz`

    private String sensitivity;
    // 📝 NOTE: Độ nhạy (dB) | Ví dụ: `88dB`

    private String impedance;
    // 📝 NOTE: Trở kháng (Ohm) | Ví dụ: `8Ω`

    private String powerHandling;
    // 📝 NOTE: Công suất chịu đựng | Ví dụ: `100W RMS`

    private String connectionType;
    // 📝 NOTE: Kết nối | Ví dụ: `Bluetooth 5.0, AUX, USB-C`

    private String voltageInput;
    // 📝 NOTE: Điện áp | Ví dụ: `5V/2A`

    private String warrantyPeriod;
    // 📝 NOTE: Thời gian bảo hành | Ví dụ: `24 tháng`

    private String warrantyType;
    // 📝 NOTE: Loại BH | Ví dụ: `1 đổi 1`, `Sửa chữa`

    private String manufacturerName;
    // 📝 NOTE: Nhà sản xuất | Ví dụ: `Harman International`

    private String manufacturerAddress;
    // 📝 NOTE: Địa chỉ NSX | Ví dụ: `123 Audio St, California, USA`

    private String productCondition;
    // 📝 NOTE: Tình trạng | Ví dụ: `New`, `Refurbished`, `Used`

    private Boolean isCustomMade;
    // 📝 NOTE: Làm theo yêu cầu | Ví dụ: `false`

    // =========================================================
    // 🔊 LOA (SPEAKER)
    // =========================================================
    private String driverConfiguration;
    // 📝 NOTE: Cấu hình driver | Ví dụ: `2-way`, `3-way`

    private String driverSize;
    // 📝 NOTE: Kích thước driver | Ví dụ: `6.5 inch woofer + 1 inch tweeter`

    private String enclosureType;
    // 📝 NOTE: Loại thùng loa | Ví dụ: `Bass Reflex`, `Sealed`

    private String coveragePattern;
    // 📝 NOTE: Góc phủ âm | Ví dụ: `180° x 180°`

    private String crossoverFrequency;
    // 📝 NOTE: Tần cắt loa | Ví dụ: `2.5kHz`

    private String placementType;
    // 📝 NOTE: Vị trí đặt | Ví dụ: `Bookshelf`, `Floorstanding`

    // =========================================================
    // 🎧 TAI NGHE (HEADPHONE)
    // =========================================================
    private String headphoneType;
    // 📝 NOTE: Loại tai nghe | Ví dụ: `Over-ear`, `On-ear`, `In-ear`

    private String compatibleDevices;
    // 📝 NOTE: Thiết bị tương thích | Ví dụ: `iPhone, Android, PC, PS5`

    private Boolean isSportsModel;
    // 📝 NOTE: Dành cho thể thao | Ví dụ: `true`

    private String headphoneFeatures;
    // 📝 NOTE: Tính năng | Ví dụ: `ANC, Touch Control, EQ App`

    private String batteryCapacity;
    // 📝 NOTE: Dung lượng pin | Ví dụ: `1000mAh`

    private Boolean hasBuiltInBattery;
    // 📝 NOTE: Có pin tích hợp | Ví dụ: `true`

    private Boolean isGamingHeadset;
    // 📝 NOTE: Tai nghe gaming | Ví dụ: `false`

    private String headphoneAccessoryType;
    // 📝 NOTE: Phụ kiện | Ví dụ: `Carrying Case, Cable`

    private String headphoneConnectionType;
    // 📝 NOTE: Kết nối | Ví dụ: `Wireless + 3.5mm`

    private String plugType;
    // 📝 NOTE: Loại jack | Ví dụ: `3.5mm L-shaped`

    private Boolean sirimApproved;
    // 📝 NOTE: Chứng nhận SIRIM (MY) | Ví dụ: `true`

    private Boolean sirimCertified;
    // 📝 NOTE: Chứng nhận SIRIM đầy đủ | Ví dụ: `true`

    private Boolean mcmcApproved;
    // 📝 NOTE: Chứng nhận MCMC (MY) | Ví dụ: `true`

    // =========================================================
    // 🎤 MICRO (MICROPHONE)
    // =========================================================
    private String micType;
    // 📝 NOTE: Loại micro | Ví dụ: `Condenser`, `Dynamic`

    private String polarPattern;
    // 📝 NOTE: Họng nhận âm | Ví dụ: `Cardioid`, `Omni`

    private String maxSPL;
    // 📝 NOTE: Âm lượng max | Ví dụ: `130dB`

    private String micOutputImpedance;
    // 📝 NOTE: Trở kháng output | Ví dụ: `150Ω`

    private String micSensitivity;
    // 📝 NOTE: Độ nhạy micro | Ví dụ: `-40dB`

    // =========================================================
    // 📻 AMPLI / RECEIVER
    // =========================================================
    private String amplifierType;
    // 📝 NOTE: Loại ampli | Ví dụ: `Class D`, `AV Receiver`

    private String totalPowerOutput;
    // 📝 NOTE: Tổng công suất | Ví dụ: `500W (8Ω)`

    private String thd;
    // 📝 NOTE: THD (méo tiếng) | Ví dụ: `0.05%`

    private String snr;
    // 📝 NOTE: SNR (tỷ lệ tín hiệu) | Ví dụ: `100dB`

    private Integer inputChannels;
    // 📝 NOTE: Kênh input | Ví dụ: `5`

    private Integer outputChannels;
    // 📝 NOTE: Kênh output | Ví dụ: `7.2`

    private Boolean supportBluetooth;
    // 📝 NOTE: Hỗ trợ Bluetooth | Ví dụ: `true`

    private Boolean supportWifi;
    // 📝 NOTE: Hỗ trợ WiFi | Ví dụ: `true`

    private Boolean supportAirplay;
    // 📝 NOTE: Hỗ trợ AirPlay | Ví dụ: `true`

    // =========================================================
    // 📀 TURNTABLE
    // =========================================================
    private String platterMaterial;
    // 📝 NOTE: Chất liệu đĩa | Ví dụ: `Aluminum`

    private String motorType;
    // 📝 NOTE: Loại động cơ | Ví dụ: `Direct Drive`

    private String tonearmType;
    // 📝 NOTE: Loại cần đĩa | Ví dụ: `S-shaped`

    private Boolean autoReturn;
    // 📝 NOTE: Tự động quay về | Ví dụ: `true`

    // =========================================================
    // 🎛️ DAC / MIXER / SOUND CARD
    // =========================================================
    private String dacChipset;
    // 📝 NOTE: Chip DAC | Ví dụ: `ESS Sabre ES9038`

    private String sampleRate;
    // 📝 NOTE: Tần mẫu | Ví dụ: `Up to 192kHz/24bit`

    private String bitDepth;
    // 📝 NOTE: Độ sâu bit | Ví dụ: `32-bit`

    private Boolean balancedOutput;
    // 📝 NOTE: Output cân bằng | Ví dụ: `true` (XLR)

    private String inputInterface;
    // 📝 NOTE: Cổng input | Ví dụ: `XLR, TRS, USB`

    private String outputInterface;
    // 📝 NOTE: Cổng output | Ví dụ: `XLR, RCA, Headphone`

    private Integer channelCount;
    // 📝 NOTE: Số kênh | Ví dụ: `8`

    private Boolean hasPhantomPower;
    // 📝 NOTE: Điện ma | Ví dụ: `true` (+48V)

    private String eqBands;
    // 📝 NOTE: Băng tần EQ | Ví dụ: `31-band`

    private String faderType;
    // 📝 NOTE: Loại fader | Ví dụ: `Motorized`

    private Boolean builtInEffects;
    // 📝 NOTE: Hiệu ứng tích hợp | Ví dụ: `true` (Reverb, Delay)

    private Boolean usbAudioInterface;
    // 📝 NOTE: Giao tiếp USB Audio | Ví dụ: `true`

    private Boolean midiSupport;
    // 📝 NOTE: Hỗ trợ MIDI | Ví dụ: `true`
}