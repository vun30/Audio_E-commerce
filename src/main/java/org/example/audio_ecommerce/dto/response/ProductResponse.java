package org.example.audio_ecommerce.dto.response;

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

    // ======================
    // 🔑 Định danh
    // ======================
    private UUID productId;
    private UUID storeId;
    private String storeName;
    private UUID categoryId;
    private String categoryName;
    private String brandName;

    // ======================
    // 🔖 Thông tin cơ bản
    // ======================
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    // ======================
    // 📸 Media
    // ======================
    private List<String> images;
    private String videoUrl;

    // ======================
    // 💵 Giá & Kho
    // ======================
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String shippingAddress;

    // ======================
    // 📊 Trạng thái
    // ======================
    private ProductStatus status;
    private Boolean isFeatured;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================
    // 🔊 Loa
    // =========================
    private String driverConfiguration;
    private String driverSize;
    private String frequencyResponse;
    private String sensitivity;
    private String impedance;
    private String powerHandling;
    private String enclosureType;
    private String coveragePattern;
    private String crossoverFrequency;
    private String placementType;
    private String connectionType;

    // =========================
    // 🎤 Micro
    // =========================
    private String micType;
    private String polarPattern;
    private String maxSPL;
    private String micOutputImpedance;
    private String micSensitivity;

    // =========================
    // 📻 Ampli
    // =========================
    private String amplifierType;
    private String totalPowerOutput;
    private String thd;
    private String snr;
    private Integer inputChannels;
    private Integer outputChannels;
    private Boolean supportBluetooth;
    private Boolean supportWifi;
    private Boolean supportAirplay;

    // =========================
    // 📀 Turntable
    // =========================
    private String platterMaterial;
    private String motorType;
    private String tonearmType;
    private Boolean autoReturn;

    // =========================
    // 🎛️ DAC
    // =========================
    private String dacChipset;
    private String sampleRate;
    private String bitDepth;
    private Boolean balancedOutput;
    private String inputInterface;
    private String outputInterface;

    // =========================
    // 🎚️ Mixer
    // =========================
    private Integer channelCount;
    private Boolean hasPhantomPower;
    private String eqBands;
    private String faderType;
    private Boolean builtInEffects;
    private Boolean usbAudioInterface;
    private Boolean midiSupport;
}
