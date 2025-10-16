package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    // =========================================================
    // 🔑 THÔNG TIN ĐỊNH DANH
    // =========================================================
    @Schema(description = "ID sản phẩm", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID productId;

    @Schema(description = "ID cửa hàng sở hữu sản phẩm", example = "b0aa8ef1-c12d-4f6a-85f1-3c2b0cfed111")
    private UUID storeId;

    @Schema(description = "Tên cửa hàng đăng bán", example = "AudioZone Vietnam")
    private String storeName;

    @Schema(description = "ID danh mục sản phẩm", example = "bafc3b6a-8321-49cc-9ff5-8378f3a5a9a4")
    private UUID categoryId;

    @Schema(description = "Tên danh mục sản phẩm", example = "Loa")
    private String categoryName;

    @Schema(description = "Tên thương hiệu", example = "Sony")
    private String brandName;

    // =========================================================
    // 🏷️ THÔNG TIN CƠ BẢN
    // =========================================================
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    // =========================================================
    // 📸 HÌNH ẢNH & VIDEO
    // =========================================================
    private List<String> images;
    private String videoUrl;

    // =========================================================
    // 💰 GIÁ CƠ BẢN & KHUYẾN MÃI
    // =========================================================
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal promotionPercent;
    private BigDecimal priceAfterPromotion;
    private BigDecimal priceBeforeVoucher;
    private BigDecimal finalPrice;
    private BigDecimal platformFeePercent;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String shippingAddress;

    // =========================================================
    // 🧮 MUA NHIỀU GIẢM GIÁ (BULK DISCOUNT)
    // =========================================================
    @Schema(description = "Danh sách khoảng giá khi mua số lượng lớn")
    private List<BulkDiscountResponse> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountResponse {
        @Schema(description = "Số lượng tối thiểu", example = "5")
        private Integer fromQuantity;

        @Schema(description = "Số lượng tối đa", example = "10")
        private Integer toQuantity;

        @Schema(description = "Đơn giá áp dụng trong khoảng", example = "950000")
        private BigDecimal unitPrice;
    }

    // =========================================================
    // 📊 TRẠNG THÁI & ĐÁNH GIÁ
    // =========================================================
    private ProductStatus status;
    private Boolean isFeatured;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================
    // 🔊 THUỘC TÍNH CHUNG
    // =========================================================
    private String frequencyResponse;
    private String sensitivity;
    private String impedance;
    private String powerHandling;
    private String connectionType;
    private String voltageInput;
    private String warrantyPeriod;
    private String warrantyType;
    private String manufacturerName;
    private String manufacturerAddress;
    private String condition;
    private Boolean isCustomMade;

    // =========================================================
    // 🔊 LOA (SPEAKER)
    // =========================================================
    private String driverConfiguration;
    private String driverSize;
    private String enclosureType;
    private String coveragePattern;
    private String crossoverFrequency;
    private String placementType;

    // =========================================================
    // 🎧 TAI NGHE (HEADPHONE)
    // =========================================================
    private String headphoneType;
    private String compatibleDevices;
    private Boolean isSportsModel;
    private String headphoneFeatures;
    private String batteryCapacity;
    private Boolean hasBuiltInBattery;
    private Boolean isGamingHeadset;
    private String headphoneAccessoryType;
    private String headphoneConnectionType;
    private String plugType;
    private Boolean sirimApproved;
    private Boolean sirimCertified;
    private Boolean mcmcApproved;

    // =========================================================
    // 🎤 MICRO (MICROPHONE)
    // =========================================================
    private String micType;
    private String polarPattern;
    private String maxSPL;
    private String micOutputImpedance;
    private String micSensitivity;

    // =========================================================
    // 📻 AMPLI / RECEIVER
    // =========================================================
    private String amplifierType;
    private String totalPowerOutput;
    private String thd;
    private String snr;
    private Integer inputChannels;
    private Integer outputChannels;
    private Boolean supportBluetooth;
    private Boolean supportWifi;
    private Boolean supportAirplay;

    // =========================================================
    // 📀 TURNTABLE
    // =========================================================
    private String platterMaterial;
    private String motorType;
    private String tonearmType;
    private Boolean autoReturn;

    // =========================================================
    // 🎛️ DAC / MIXER / SOUND CARD
    // =========================================================
    private String dacChipset;
    private String sampleRate;
    private String bitDepth;
    private Boolean balancedOutput;
    private String inputInterface;
    private String outputInterface;
    private Integer channelCount;
    private Boolean hasPhantomPower;
    private String eqBands;
    private String faderType;
    private Boolean builtInEffects;
    private Boolean usbAudioInterface;
    private Boolean midiSupport;
}
