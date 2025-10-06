package org.example.audio_ecommerce.entity;

import org.example.audio_ecommerce.entity.Enum.Category;
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
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "product_id", columnDefinition = "CHAR(36)")
    private UUID productId;

    // ======================
    // 🔗 QUAN HỆ
    // ======================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "brand_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID brandId;

    // ======================
    // 🔖 THÔNG TIN CƠ BẢN
    // ======================
    private String name;
    @Column(unique = true)
    private String slug;
    private String shortDescription;
    @Lob
    private String description;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    // ======================
    // 📸 MEDIA
    // ======================
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images;
    private String videoUrl;

    // ======================
    // 💵 GIÁ & KHO
    // ======================
    @Column(unique = true, nullable = false)
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    @Column(length = 500)
    private String shippingAddress;

    // ======================
    // 📊 TRẠNG THÁI
    // ======================
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private Boolean isFeatured;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private UUID createdBy;
    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private UUID updatedBy;

    // =========================
    // 🔊 THUỘC TÍNH KỸ THUẬT - LOA
    // =========================
     private String driverConfiguration;   // Cấu hình driver
    private String driverSize;            // Kích thước driver
    private String frequencyResponse;     // Dải tần
    private String sensitivity;           // Độ nhạy
    private String impedance;             // Trở kháng
    private String powerHandling;         // Công suất chịu tải
    private String enclosureType;         // Kiểu thùng loa
    private String coveragePattern;       // Góc phủ âm
    private String crossoverFrequency;    // Tần số cắt
    private String placementType;         // Kiểu đặt (floorstanding, bookshelf, wall-mount...)
    private String connectionType;        // 🔌 Loại kết nối: Bluetooth, Optical, RCA, AUX, Wi-Fi...

    // =========================
    // 🎤 THUỘC TÍNH MICRO
    // =========================
    private String micType;               // Dynamic / Condenser
    private String polarPattern;          // Cardioid, Omni, Bidirectional...
    private String maxSPL;                // Mức áp suất âm tối đa
    private String micOutputImpedance;    // Trở kháng đầu ra
    private String micSensitivity;        // Độ nhạy micro

    // =========================
    // 📻 MÁY THU (Receiver) / AMPLI
    // =========================
    private String amplifierType;         // Class A/B/D/G
    private String totalPowerOutput;      // Tổng công suất
    private String thd;                   // Tổng méo hài (THD)
    private String snr;                   // Tỷ lệ tín hiệu / nhiễu
    private Integer inputChannels;        // Số kênh đầu vào
    private Integer outputChannels;       // Số kênh đầu ra
    private Boolean supportBluetooth;
    private Boolean supportWifi;
    private Boolean supportAirplay;

    // =========================
    // 📀 MÁY ĐỌC ĐĨA THAN (Turntable)
    // =========================
    private String platterMaterial;       // Vật liệu mâm xoay
    private String motorType;             // Loại motor (belt / direct)
    private String tonearmType;           // Loại cần
    private Boolean autoReturn;           // Tự động trả cần

    // =========================
    // 🎛️ DAC / SOUND CARD
    // =========================
    private String dacChipset;            // Chip DAC
    private String sampleRate;            // Tần số lấy mẫu
    private String bitDepth;              // Độ sâu bit
    private Boolean balancedOutput;       // Có hỗ trợ balanced không
    private String inputInterface;        // USB, Optical, Coaxial...
    private String outputInterface;       // RCA, XLR, 6.5mm...

    // =========================
    // 🎚️ MIXER / EQ / DJ CONTROLLER
    // =========================
    private Integer channelCount;         // Số kênh mixer
    private Boolean hasPhantomPower;      // Có hỗ trợ phantom 48V không
    private String eqBands;               // Số dải EQ
    private String faderType;             // Loại fader (linear, rotary)
    private Boolean builtInEffects;       // Có hiệu ứng tích hợp không
    private Boolean usbAudioInterface;    // Có giao diện USB không
    private Boolean midiSupport;          // Có hỗ trợ MIDI không
}
