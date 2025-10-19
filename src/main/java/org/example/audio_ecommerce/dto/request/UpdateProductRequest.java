package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {

    // =========================================================
    // 🔗 LIÊN KẾT & DANH MỤC
    // =========================================================
    @Schema(
            description = "Tên danh mục sản phẩm (BE tự ánh xạ sang Category tương ứng)",
            example = "Loa",
            allowableValues = {
                    "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp",
                    "Turntable", "Sound Card", "DJ Controller", "Combo"
            }
    )
    private String categoryName;

    @Schema(description = "Tên thương hiệu", example = "Sony")
    private String brandName;

    @Schema(description = "Mã SKU (phải duy nhất trong store)", example = "SONY-SPK-001")
    private String sku;

    // =========================================================
    // 🏷️ THÔNG TIN CƠ BẢN
    // =========================================================
    @Schema(description = "Tên sản phẩm", example = "Sony SRS-XB33 Extra Bass")
    private String name;

    @Schema(description = "Mô tả ngắn", example = "Loa Bluetooth chống nước, âm trầm mạnh mẽ")
    private String shortDescription;

    @Schema(description = "Mô tả chi tiết sản phẩm (HTML hoặc text)")
    private String description;

    @Schema(description = "Mã model", example = "SRS-XB33")
    private String model;

    @Schema(description = "Màu sắc", example = "Đen")
    private String color;

    @Schema(description = "Chất liệu vỏ", example = "Nhựa ABS cao cấp")
    private String material;

    @Schema(description = "Kích thước (Dài x Rộng x Cao)", example = "24cm x 10cm x 12cm")
    private String dimensions;

    @Schema(description = "Trọng lượng (kg)", example = "1.2")
    private BigDecimal weight;

    @Schema(description = "Danh sách URL hình ảnh sản phẩm")
    private List<String> images;

    @Schema(description = "Video mô tả sản phẩm", example = "https://youtube.com/xyz123")
    private String videoUrl;

    @Schema(description = "Danh sách biến thể sản phẩm (VD: màu sắc, dung lượng, size...)")
    private List<Product.ProductVariant> variants;

    // =========================================================
    // 💰 GIÁ & KHO
    // =========================================================
    @Schema(description = "Giá gốc của sản phẩm", example = "1500000")
    private BigDecimal price;

    @Schema(description = "Loại tiền tệ", example = "VND")
    private String currency;

    @Schema(description = "Số lượng tồn kho", example = "50")
    private Integer stockQuantity;

    @Schema(description = "Địa chỉ kho hàng", example = "HCM - Quận 7")
    private String warehouseLocation;

        // =========================================================
    // 🌍 ĐỊA CHỈ HÀNH CHÍNH (CODE TỈNH, QUẬN, XÃ)
    // =========================================================
    @Schema(description = "Mã tỉnh/thành phố", example = "01 Hà Nội")
    private String provinceCode;
    // 📝 NOTE: Mã tỉnh/thành phố | Ví dụ: "01" (Hà Nội), "79" (TP.HCM)

    @Schema(description = "Mã quận/huyện", example = "760")
    private String districtCode;
    // 📝 NOTE: Mã quận/huyện | Ví dụ: "760" (Quận 1)

    @Schema(description = "Mã phường/xã", example = "26734")
    private String wardCode;
    // 📝 NOTE: Mã phường/xã | Ví dụ: "26734" (Phường Bến Nghé)

    @Schema(description = "Địa chỉ giao hàng / xuất kho")
    private String shippingAddress;

    @Schema(description = "Phí vận chuyển mặc định", example = "30000")
    private BigDecimal shippingFee;

    @Schema(description = "Danh sách ID phương thức vận chuyển được hỗ trợ")
    private List<UUID> supportedShippingMethodIds;

    // =========================================================
    // 🧮 MUA NHIỀU GIẢM GIÁ
    // =========================================================
    @Schema(description = "Danh sách mức giảm giá theo số lượng mua")
    private List<BulkDiscountRequest> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountRequest {
        @Schema(description = "Số lượng tối thiểu", example = "5")
        private Integer fromQuantity;

        @Schema(description = "Số lượng tối đa", example = "10")
        private Integer toQuantity;

        @Schema(description = "Giá khi mua trong khoảng", example = "900000")
        private BigDecimal unitPrice;
    }

    // =========================================================
    // 📊 TRẠNG THÁI
    // =========================================================
    @Schema(description = "Trạng thái sản phẩm", example = "ACTIVE")
    private ProductStatus status;

    @Schema(description = "Sản phẩm nổi bật", example = "false")
    private Boolean isFeatured;

    // =========================================================
    // ⚙️ KỸ THUẬT & BẢO HÀNH
    // =========================================================
    private String voltageInput;
    private String warrantyPeriod;
    private String warrantyType;
    private String manufacturerName;
    private String manufacturerAddress;
    private String productCondition;
    private Boolean isCustomMade;

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
    // 🔊 LOA
    // =========================================================
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
    // 🎤 MICRO
    // =========================================================
    private String micType;
    private String polarPattern;
    private String maxSPL;
    private String micOutputImpedance;
    private String micSensitivity;

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
