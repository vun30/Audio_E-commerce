package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // ======================
    // 🔗 Liên kết & Danh mục
    // ======================
    @Schema(
            description = "Tên danh mục sản phẩm (BE tự ánh xạ sang categoryId tương ứng)",
            example = "Loa",
            allowableValues = {
                    "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp",
                    "Turntable", "Sound Card", "DJ Controller", "Combo"
            }
    )
    private String categoryName;

    @Schema(description = "Tên thương hiệu", example = "Sony")
    private String brandName;

    @Schema(description = "Mã SKU của sản phẩm", example = "SONY-SPK-001")
    private String sku;

    // ======================
    // 🔖 Thông tin cơ bản
    // ======================
    @Schema(description = "Tên sản phẩm", example = "Sony SRS-XB33 Extra Bass")
    private String name;

    @Schema(description = "Mô tả ngắn", example = "Loa Bluetooth chống nước, âm trầm mạnh mẽ")
    private String shortDescription;

    @Schema(description = "Mô tả chi tiết sản phẩm (HTML hoặc text)")
    private String description;

    @Schema(description = "Mã model", example = "SRS-XB33")
    private String model;

    @Schema(description = "Màu sắc sản phẩm", example = "Đen")
    private String color;

    @Schema(description = "Chất liệu vỏ", example = "Nhựa ABS cao cấp")
    private String material;

    @Schema(description = "Kích thước (Dài x Rộng x Cao)", example = "24cm x 10cm x 12cm")
    private String dimensions;

    @Schema(description = "Trọng lượng (kg)", example = "1.2")
    private BigDecimal weight;

    // ======================
    // 📸 Media
    // ======================
    @Schema(description = "Danh sách URL hình ảnh sản phẩm")
    private List<String> images;

    @Schema(description = "Video mô tả sản phẩm", example = "https://youtube.com/xyz123")
    private String videoUrl;

    // ======================
    // 💰 Giá & Khuyến mãi
    // ======================
    @Schema(description = "Giá gốc của sản phẩm", example = "1500000")
    private BigDecimal price;

    @Schema(description = "Loại tiền tệ", example = "VND")
    private String currency;

    @Schema(description = "Số lượng tồn kho hiện tại", example = "50")
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

    // =========================================================
// 🚚 VẬN CHUYỂN
// =========================================================
    @Schema(description = "Phí vận chuyển mặc định", example = "30000")
    private BigDecimal shippingFee;

    @Schema(description = "Danh sách ID phương thức vận chuyển được hỗ trợ")
    private List<UUID> supportedShippingMethodIds;

    // =========================================================
// 🧩 BIẾN THỂ SẢN PHẨM
// =========================================================
    @Schema(description = "Danh sách biến thể của sản phẩm (VD: màu sắc, dung lượng, size, ...)")
    private List<Product.ProductVariant> variants;

    // ======================
    // 🧮 Mua nhiều giảm giá (Bulk Discounts)
    // ======================
    @Schema(description = "Danh sách các mức giảm giá theo số lượng mua")
    private List<BulkDiscountRequest> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountRequest {
        @Schema(description = "Số lượng tối thiểu để áp dụng mức giá này", example = "5")
        private Integer fromQuantity;

        @Schema(description = "Số lượng tối đa cho mức giá này", example = "10")
        private Integer toQuantity;

        @Schema(description = "Đơn giá khi mua trong khoảng này", example = "900000")
        private BigDecimal unitPrice;
    }

    // ======================
    // ⚙️ Kỹ thuật chung / Bảo hành
    // ======================
    @Schema(description = "Điện áp đầu vào", example = "5V")
    private String voltageInput;

    @Schema(description = "Thời gian bảo hành", example = "12 tháng")
    private String warrantyPeriod;

    @Schema(description = "Loại bảo hành", example = "Chính hãng")
    private String warrantyType;

    @Schema(description = "Tên nhà sản xuất", example = "Sony Corporation")
    private String manufacturerName;

    @Schema(description = "Địa chỉ nhà sản xuất", example = "Tokyo, Japan")
    private String manufacturerAddress;

    @Schema(description = "Tình trạng sản phẩm (Mới / Like New)", example = "Mới 100%")
    private String productCondition;

    @Schema(description = "Sản phẩm đặt riêng theo yêu cầu", example = "false")
    private Boolean isCustomMade;

    // ======================
    // 🎧 Tai nghe (Headphone)
    // ======================
    @Schema(description = "Loại tai nghe", example = "Over-ear")
    private String headphoneType;

    @Schema(description = "Thiết bị tương thích", example = "PC, điện thoại, TV")
    private String compatibleDevices;

    @Schema(description = "Tai nghe thể thao chuyên dụng", example = "false")
    private Boolean isSportsModel;

    @Schema(description = "Tính năng tai nghe", example = "Chống ồn, Mic đàm thoại, Bluetooth 5.3")
    private String headphoneFeatures;

    @Schema(description = "Dung lượng pin (nếu có)", example = "800mAh")
    private String batteryCapacity;

    @Schema(description = "Có pin tích hợp hay không", example = "true")
    private Boolean hasBuiltInBattery;

    @Schema(description = "Tai nghe chuyên dụng cho game", example = "false")
    private Boolean isGamingHeadset;

    @Schema(description = "Phụ kiện đi kèm", example = "Earpad, Cable Type-C")
    private String headphoneAccessoryType;

    @Schema(description = "Cổng kết nối tai nghe", example = "Type-C, 3.5mm")
    private String headphoneConnectionType;

    @Schema(description = "Loại đầu cắm", example = "3.5mm")
    private String plugType;

    @Schema(description = "SIRIM Approved (Chứng nhận)", example = "false")
    private Boolean sirimApproved;

    @Schema(description = "SIRIM Certified (Đã chứng nhận)", example = "false")
    private Boolean sirimCertified;

    @Schema(description = "MCMC Approved", example = "false")
    private Boolean mcmcApproved;

    // ======================
    // 🔊 Loa (Speaker)
    // ======================
    @Schema(description = "Cấu hình driver", example = "2-way")
    private String driverConfiguration;

    @Schema(description = "Kích thước driver", example = "6.5 inch")
    private String driverSize;

    @Schema(description = "Dải tần đáp ứng", example = "20Hz - 20000Hz")
    private String frequencyResponse;

    @Schema(description = "Độ nhạy (dB)", example = "90")
    private String sensitivity;

    @Schema(description = "Trở kháng (Ohm)", example = "8")
    private String impedance;

    @Schema(description = "Công suất chịu tải", example = "100W")
    private String powerHandling;

    @Schema(description = "Kiểu thùng loa", example = "Bass Reflex")
    private String enclosureType;

    @Schema(description = "Góc phủ âm", example = "90° x 60°")
    private String coveragePattern;

    @Schema(description = "Tần số cắt", example = "2.5kHz")
    private String crossoverFrequency;

    @Schema(description = "Kiểu đặt loa", example = "Bookshelf")
    private String placementType;

    @Schema(description = "Kiểu kết nối", example = "Bluetooth, Optical, RCA")
    private String connectionType;

    // ======================
    // 📻 Ampli / Receiver
    // ======================
    @Schema(description = "Loại ampli", example = "Class D")
    private String amplifierType;

    @Schema(description = "Tổng công suất", example = "2x50W")
    private String totalPowerOutput;

    @Schema(description = "Tổng méo hài (THD)", example = "<0.01%")
    private String thd;

    @Schema(description = "Tỷ lệ SNR (Signal to Noise Ratio)", example = "100dB")
    private String snr;

    @Schema(description = "Số kênh đầu vào", example = "4")
    private Integer inputChannels;

    @Schema(description = "Số kênh đầu ra", example = "2")
    private Integer outputChannels;

    @Schema(description = "Hỗ trợ Bluetooth", example = "true")
    private Boolean supportBluetooth;

    @Schema(description = "Hỗ trợ WiFi", example = "true")
    private Boolean supportWifi;

    @Schema(description = "Hỗ trợ AirPlay", example = "true")
    private Boolean supportAirplay;

    // ======================
    // 🎤 Micro
    // ======================
    @Schema(description = "Loại micro", example = "Dynamic")
    private String micType;

    @Schema(description = "Hướng thu âm", example = "Cardioid")
    private String polarPattern;

    @Schema(description = "Mức SPL tối đa", example = "130dB")
    private String maxSPL;

    @Schema(description = "Trở kháng đầu ra của micro", example = "150 Ohm")
    private String micOutputImpedance;

    @Schema(description = "Độ nhạy micro", example = "-40dB")
    private String micSensitivity;

    // ======================
    // 📀 Turntable
    // ======================
    @Schema(description = "Vật liệu mâm xoay", example = "Nhôm")
    private String platterMaterial;

    @Schema(description = "Loại motor", example = "Belt Drive")
    private String motorType;

    @Schema(description = "Loại cần", example = "S-shape")
    private String tonearmType;

    @Schema(description = "Tự động trả cần", example = "false")
    private Boolean autoReturn;

    // ======================
    // 🎛️ DAC / Mixer / Sound Card
    // ======================
    @Schema(description = "Chip DAC", example = "ESS9038")
    private String dacChipset;

    @Schema(description = "Tần số lấy mẫu", example = "192kHz")
    private String sampleRate;

    @Schema(description = "Độ sâu bit", example = "24-bit")
    private String bitDepth;

    @Schema(description = "Hỗ trợ Balanced Output", example = "true")
    private Boolean balancedOutput;

    @Schema(description = "Giao diện đầu vào", example = "USB, Optical")
    private String inputInterface;

    @Schema(description = "Giao diện đầu ra", example = "RCA, XLR")
    private String outputInterface;

    @Schema(description = "Số kênh mixer", example = "4")
    private Integer channelCount;

    @Schema(description = "Có hỗ trợ nguồn 48V cho micro", example = "true")
    private Boolean hasPhantomPower;

    @Schema(description = "Số dải EQ", example = "3-band")
    private String eqBands;

    @Schema(description = "Loại fader", example = "Linear")
    private String faderType;

    @Schema(description = "Có hiệu ứng tích hợp sẵn", example = "true")
    private Boolean builtInEffects;

    @Schema(description = "Có giao diện USB Audio", example = "true")
    private Boolean usbAudioInterface;

    @Schema(description = "Có hỗ trợ MIDI", example = "true")
    private Boolean midiSupport;
}
