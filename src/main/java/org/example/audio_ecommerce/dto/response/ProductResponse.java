package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Product;

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

    @Schema(description = "ID cửa hàng sở hữu sản phẩm")
    private UUID storeId;

    @Schema(description = "Tên cửa hàng sở hữu sản phẩm")
    private String storeName;

    @Schema(description = "ID danh mục sản phẩm")
    private UUID categoryId;

    @Schema(description = "Tên danh mục sản phẩm")
    private String categoryName;

    @Schema(description = "Thương hiệu sản phẩm", example = "Sony")
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
    // 🧩 BIẾN THỂ
    // =========================================================
    @Schema(description = "Danh sách biến thể sản phẩm (VD: màu sắc, dung lượng, size)")
    private List<Product.ProductVariant> variants;

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
    private BigDecimal voucherAmount;
    private BigDecimal finalPrice;
    private BigDecimal platformFeePercent;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String shippingAddress;

    // =========================================================
    // 🚚 VẬN CHUYỂN
    // =========================================================
    @Schema(description = "Phí vận chuyển mặc định", example = "30000")
    private BigDecimal shippingFee;

    @Schema(description = "Danh sách ID phương thức vận chuyển hỗ trợ")
    private List<UUID> supportedShippingMethodIds;

    // =========================================================
    // 🧮 MUA NHIỀU GIẢM GIÁ
    // =========================================================
    @Schema(description = "Danh sách các mức giá giảm khi mua số lượng lớn")
    private List<BulkDiscountResponse> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountResponse {
        @Schema(description = "Số lượng tối thiểu để áp dụng", example = "5")
        private Integer fromQuantity;

        @Schema(description = "Số lượng tối đa cho mức giá này", example = "10")
        private Integer toQuantity;

        @Schema(description = "Giá áp dụng trong khoảng này", example = "950000")
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
    private LocalDateTime lastUpdatedAt;
    private Long lastUpdateIntervalDays;
    private UUID createdBy;
    private UUID updatedBy;

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
    private String productCondition;
    private Boolean isCustomMade;

    // =========================================================
    // 🔊 LOA
    // =========================================================
    private String driverConfiguration;
    private String driverSize;
    private String enclosureType;
    private String coveragePattern;
    private String crossoverFrequency;
    private String placementType;

    // =========================================================
    // 🎧 TAI NGHE
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
    // 🎤 MICRO
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
