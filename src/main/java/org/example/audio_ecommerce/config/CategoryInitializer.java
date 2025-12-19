package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.entity.CategoryAttributeOption;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;
import org.example.audio_ecommerce.repository.CategoryAttributeRepository;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryInitializer {

    private final CategoryRepository categoryRepo;
    private final CategoryAttributeRepository attrRepo;

    @PostConstruct
    public void initDefaultCategories() {

        if (categoryRepo.count() > 0) {
            log.info("📌 Category already exists → Skip initialization");
            return;
        }

        log.info("🚀 Initializing default Categories...");

        // =====================================================
// 1) LOA – SPEAKER (ĐỦ CHO RECOMMEND + MÔ PHỎNG)
// - User chỉ nhập số (không nhập đơn vị)
// - Đơn vị hiển thị nằm trong attributeLabel
// =====================================================
        createCategory(
                "Loa",
                List.of(
                        // ====== BẮT BUỘC (LÕI) ======

                        // Loại loa: dùng cho filter + preset directivity
                        att("speakerType", "Loại loa", CategoryAttributeDataType.STRING,
                                List.of("Loa bookshelf", "Loa đứng", "Loa center", "Loa subwoofer")),

                        // dB / 1W / 1m
                        att("sensitivityDb", "Độ nhạy (dB/1W/1m)", CategoryAttributeDataType.NUMBER, List.of()),

                        // W
                        att("powerRmsW", "Công suất RMS (W)", CategoryAttributeDataType.NUMBER, List.of()),

                        // Ω (nhập số 2/4/6/8/16)
                        att("impedanceOhm", "Trở kháng (Ω)", CategoryAttributeDataType.NUMBER,
                                List.of("2", "4", "6", "8", "16")),

                        // Hz
                        att("frequencyLowHz", "Tần số thấp nhất (Hz)", CategoryAttributeDataType.NUMBER, List.of()),
                        att("frequencyHighHz", "Tần số cao nhất (Hz)", CategoryAttributeDataType.NUMBER, List.of()),

                        // % (khuyến nghị có)
                        att("thdPercent", "Độ méo tiếng tổng THD (%)", CategoryAttributeDataType.NUMBER, List.of()),

                        // ====== OPTIONAL CHO HƯỚNG PHỦ ÂM (nếu có data hãng) ======
                        att("dispersionHorizontalDeg", "Góc phủ âm ngang (độ)", CategoryAttributeDataType.NUMBER, List.of()),
                        att("dispersionVerticalDeg", "Góc phủ âm dọc (độ)", CategoryAttributeDataType.NUMBER, List.of()),

                        // ====== MÔ TẢ / FILTER (KHÔNG BẮT BUỘC CHO TÍNH TOÁN) ======
                        att("driverConfiguration", "Cấu hình loa", CategoryAttributeDataType.STRING,
                                List.of("1 đường tiếng", "2 đường tiếng", "3 đường tiếng", "4 đường tiếng", "Đồng trục")),

                        // inch (user nhập số, ví dụ 6.5)
                        att("driverSizeInch", "Kích thước củ loa (inch)", CategoryAttributeDataType.NUMBER, List.of()),

                        att("enclosureType", "Loại thùng loa", CategoryAttributeDataType.STRING,
                                List.of("Thùng kín", "Thùng hở", "Bass reflex", "Bandpass", "Transmission Line")),

                        // Hz
                        att("crossoverFrequencyHz", "Tần số cắt (Hz)", CategoryAttributeDataType.NUMBER, List.of())
                )
        );

        // =====================================================
        // 2) TAI NGHE – HEADPHONE
        // =====================================================
        createCategory(
                "Tai nghe",
                List.of(
                        att("frequencyResponse", "Dải tần", CategoryAttributeDataType.STRING, List.of()),

                        att("sensitivity", "Độ nhạy", CategoryAttributeDataType.STRING, List.of()),

                        att("impedance", "Trở kháng", CategoryAttributeDataType.STRING,
                                List.of("16Ω", "24Ω", "32Ω", "64Ω", "80Ω", "250Ω", "300Ω", "600Ω")),

                        att("headphoneType", "Loại tai nghe", CategoryAttributeDataType.STRING,
                                List.of("In-ear", "On-ear", "Over-ear", "True Wireless",
                                        "Wireless Bluetooth", "Gaming Headset", "Studio Monitor",
                                        "Open-back", "Closed-back")),

                        att("compatibleDevices", "Thiết bị tương thích", CategoryAttributeDataType.STRING,
                                List.of("PC", "Laptop", "Android", "iOS", "MacOS",
                                        "PlayStation", "Xbox", "Nintendo Switch")),

                        att("headphoneFeatures", "Tính năng", CategoryAttributeDataType.STRING,
                                List.of("Active Noise Cancelling", "Passive Noise Cancelling",
                                        "Hi-Res Audio", "Built-in Microphone", "Touch Control",
                                        "Low Latency", "Water Resistant", "Dual Device Connection",
                                        "7.1 Surround")),

                        att("batteryCapacity", "Dung lượng pin", CategoryAttributeDataType.STRING, List.of())
                )
        );

        // =====================================================
        // 3) MICRO
        // =====================================================
        createCategory(
                "Micro",
                List.of(
                        att("micType", "Loại micro", CategoryAttributeDataType.STRING,
                                List.of("Dynamic", "Condenser", "Ribbon", "Lavalier",
                                        "Shotgun", "USB Microphone", "Broadcast Microphone")),

                        att("polarPattern", "Họng nhận âm", CategoryAttributeDataType.STRING,
                                List.of("Cardioid", "Supercardioid", "Hypercardioid",
                                        "Omnidirectional", "Bidirectional (Figure-8)", "Multi-pattern")),

                        att("maxSPL", "Mức áp suất âm tối đa", CategoryAttributeDataType.STRING, List.of()),

                        att("micOutputImpedance", "Trở kháng output", CategoryAttributeDataType.STRING,
                                List.of("50Ω", "150Ω", "200Ω", "250Ω", "600Ω")),

                        att("micSensitivity", "Độ nhạy mic", CategoryAttributeDataType.STRING, List.of())
                )
        );

        // =====================================================
        // 4) AMPLIFIER
        // =====================================================
        createCategory(
                "Ampli",
                List.of(
                        att("amplifierType", "Loại ampli", CategoryAttributeDataType.STRING,
                                List.of("Class A", "Class AB", "Class B", "Class D",
                                        "Class H", "Hybrid Tube/Transistor", "Tube Amplifier")),

                        att("totalPowerOutput", "Tổng công suất", CategoryAttributeDataType.STRING, List.of()),

                        att("thd", "Độ méo tiếng (THD)", CategoryAttributeDataType.STRING, List.of()),

                        att("snr", "Tỷ lệ SNR", CategoryAttributeDataType.STRING, List.of()),

                        att("inputChannels", "Kênh input", CategoryAttributeDataType.NUMBER, List.of()),

                        att("outputChannels", "Kênh output", CategoryAttributeDataType.STRING,
                                List.of("Mono", "2-Channel (Stereo)", "4-Channel",
                                        "5.1 Channel", "7.1 Channel", "9.1 Channel", "11.1 Channel")),

                        att("supportBluetooth", "Hỗ trợ Bluetooth", CategoryAttributeDataType.BOOLEAN, List.of()),

                        att("supportWifi", "Hỗ trợ WiFi", CategoryAttributeDataType.BOOLEAN, List.of()),

                        att("supportAirplay", "Hỗ trợ AirPlay", CategoryAttributeDataType.BOOLEAN, List.of())
                )
        );

        // =====================================================
        // 5) TURNTABLE
        // =====================================================
        createCategory(
                "Turntable",
                List.of(
                        att("platterMaterial", "Chất liệu mâm đĩa", CategoryAttributeDataType.STRING,
                                List.of("Aluminum", "Acrylic", "Glass", "Wood",
                                        "Steel", "Carbon Fiber")),

                        att("motorType", "Loại động cơ", CategoryAttributeDataType.STRING,
                                List.of("Belt-drive", "Direct-drive", "Idler-wheel")),

                        att("tonearmType", "Loại tay cần", CategoryAttributeDataType.STRING,
                                List.of("S-shaped", "Straight", "J-shaped",
                                        "Static Balance", "Dynamic Balance")),

                        att("autoReturn", "Tự động trả cần", CategoryAttributeDataType.BOOLEAN, List.of())
                )
        );

        // =====================================================
        // 6) DAC / MIXER / SOUNDCARD
        // =====================================================
        createCategory(
                "DAC / Mixer / Soundcard",
                List.of(
                        att("dacChipset", "Chip DAC", CategoryAttributeDataType.STRING,
                                List.of("ESS Sabre", "AKM Velvet Sound", "Cirrus Logic",
                                        "Burr-Brown", "Wolfson")),

                        att("sampleRate", "Tần mẫu", CategoryAttributeDataType.STRING,
                                List.of("44.1 kHz", "48 kHz", "96 kHz", "192 kHz",
                                        "384 kHz", "768 kHz")),

                        att("bitDepth", "Độ sâu bit", CategoryAttributeDataType.STRING,
                                List.of("16-bit", "24-bit", "32-bit")),

                        att("balancedOutput", "Output cân bằng (XLR)", CategoryAttributeDataType.BOOLEAN, List.of()),

                        att("inputInterface", "Cổng input", CategoryAttributeDataType.STRING,
                                List.of("USB", "USB-C", "Optical", "Coaxial",
                                        "XLR", "TRS", "RCA")),

                        att("outputInterface", "Cổng output", CategoryAttributeDataType.STRING,
                                List.of("RCA", "XLR", "TRS", "6.35mm", "3.5mm")),

                        att("channelCount", "Số kênh", CategoryAttributeDataType.NUMBER,
                                List.of("1", "2", "4", "6", "8", "12", "16", "24")),

                        att("hasPhantomPower", "Nguồn phantom +48V", CategoryAttributeDataType.BOOLEAN, List.of()),

                        att("eqBands", "Dải EQ", CategoryAttributeDataType.STRING,
                                List.of("2-band", "3-band", "5-band", "7-band", "10-band")),

                        att("faderType", "Loại fader", CategoryAttributeDataType.STRING,
                                List.of("Linear Fader", "Rotary Fader", "Crossfader"))
                )
        );

        log.info("🎉 Default Categories initialized successfully!");
    }


    // =====================================================================
    // HELPER — CREATE ATTRIBUTE + OPTIONS
    // =====================================================================
    private CategoryAttribute att(String name, String label, CategoryAttributeDataType type, List<String> options) {

        CategoryAttribute attr = CategoryAttribute.builder()
                .attributeName(name)
                .attributeLabel(label)
                .dataType(type)
                .options(new ArrayList<>())
                .build();

        for (String op : options) {
            attr.getOptions().add(
                    CategoryAttributeOption.builder()
                            .attribute(attr)
                            .optionValue(op)
                            .build()
            );
        }

        return attr;
    }

    // =====================================================================
    // HELPER — CREATE CATEGORY
    // =====================================================================
    private void createCategory(String name, List<CategoryAttribute> attributes) {

        if (categoryRepo.existsByNameIgnoreCase(name)) {
            log.info("⚠ Category '{}' already exists → skip", name);
            return;
        }

        Category cate = categoryRepo.save(
                Category.builder()
                        .name(name)
                        .parent(null)
                        .build()
        );

        for (CategoryAttribute attr : attributes) {
            attr.setCategory(cate);
            attrRepo.save(attr);
        }

        log.info("✅ Category '{}' created ({} attributes)", name, attributes.size());
    }
}
