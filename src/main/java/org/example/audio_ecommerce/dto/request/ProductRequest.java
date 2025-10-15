package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // ======================
    // 🔗 Liên kết
    // ======================
    @Schema(
        description = "Tên danh mục sản phẩm (BE tự tìm categoryId trong DB)",
        example = "Loa",
        allowableValues = {
            "Loa", "Micro", "DAC", "Mixer", "Amp",
            "Turntable", "Sound Card", "DJ Controller", "Combo"
        }
    )
    private String categoryName;   // 🟢 FE chọn theo tên — BE tự tìm Category

    private String brandName;      // 🏷️ Tên thương hiệu

    // ======================
    // 🔖 Thông tin cơ bản
    // ======================
    private String name;
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
    private List<String> images;   // Danh sách URL ảnh
    private String videoUrl;       // URL video giới thiệu

    // ======================
    // 💵 Giá & Kho
    // ======================
    private String sku;            // FE nhập
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String shippingAddress;

    // =========================
    // 🔊 Loa (Speaker)
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
    // 🎤 Microphone
    // =========================
    private String micType;
    private String polarPattern;
    private String maxSPL;
    private String micOutputImpedance;
    private String micSensitivity;

    // =========================
    // 📻 Receiver / Amplifier
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
    // 🎛️ DAC / Sound Card
    // =========================
    private String dacChipset;
    private String sampleRate;
    private String bitDepth;
    private Boolean balancedOutput;
    private String inputInterface;
    private String outputInterface;

    // =========================
    // 🎚️ Mixer / DJ Controller
    // =========================
    private Integer channelCount;
    private Boolean hasPhantomPower;
    private String eqBands;
    private String faderType;
    private Boolean builtInEffects;
    private Boolean usbAudioInterface;
    private Boolean midiSupport;
}
