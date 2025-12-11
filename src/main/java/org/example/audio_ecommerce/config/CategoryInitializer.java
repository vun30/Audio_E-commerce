package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.repository.CategoryAttributeRepository;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Component;

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

        // ================================================
        // 1) LOA (SPEAKER)
        // ================================================
        createCategory(
                "Loa",
                List.of(
                        att("frequencyResponse", "Dải tần", "STRING"),
                        att("sensitivity", "Độ nhạy", "STRING"),
                        att("impedance", "Trở kháng", "STRING"),
                        att("powerHandling", "Công suất chịu đựng", "STRING"),
                        att("driverConfiguration", "Cấu hình driver", "STRING"),
                        att("driverSize", "Kích thước driver", "STRING"),
                        att("enclosureType", "Loại thùng loa", "STRING"),
                        att("coveragePattern", "Góc phủ âm", "STRING"),
                        att("crossoverFrequency", "Tần cắt", "STRING")
                )
        );

        // ================================================
        // 2) TAI NGHE (HEADPHONE)
        // ================================================
        createCategory(
                "Tai nghe",
                List.of(
                        att("frequencyResponse", "Dải tần", "STRING"),
                        att("sensitivity", "Độ nhạy", "STRING"),
                        att("impedance", "Trở kháng", "STRING"),
                        att("headphoneType", "Loại tai nghe", "STRING"),
                        att("compatibleDevices", "Thiết bị tương thích", "STRING"),
                        att("headphoneFeatures", "Tính năng", "STRING"),
                        att("batteryCapacity", "Dung lượng pin", "STRING")
                )
        );

        // ================================================
        // 3) MICRO
        // ================================================
        createCategory(
                "Micro",
                List.of(
                        att("micType", "Loại micro", "STRING"),
                        att("polarPattern", "Hộng nhận âm", "STRING"),
                        att("maxSPL", "Mức áp suất âm tối đa", "STRING"),
                        att("micOutputImpedance", "Trở kháng output", "STRING"),
                        att("micSensitivity", "Độ nhạy mic", "STRING")
                )
        );

        // ================================================
        // 4) AMPLI
        // ================================================
        createCategory(
                "Ampli",
                List.of(
                        att("amplifierType", "Loại ampli", "STRING"),
                        att("totalPowerOutput", "Tổng công suất", "STRING"),
                        att("thd", "Độ méo tiếng (THD)", "STRING"),
                        att("snr", "Tỷ lệ SNR", "STRING"),
                        att("inputChannels", "Kênh input", "NUMBER"),
                        att("outputChannels", "Kênh output", "STRING"),
                        att("supportBluetooth", "Hỗ trợ Bluetooth", "BOOLEAN"),
                        att("supportWifi", "Hỗ trợ WiFi", "BOOLEAN"),
                        att("supportAirplay", "Hỗ trợ AirPlay", "BOOLEAN")
                )
        );

        // ================================================
        // 5) TURNTABLE
        // ================================================
        createCategory(
                "Turntable",
                List.of(
                        att("platterMaterial", "Chất liệu mâm đĩa", "STRING"),
                        att("motorType", "Loại động cơ", "STRING"),
                        att("tonearmType", "Loại tay cần", "STRING"),
                        att("autoReturn", "Tự động trả cần", "BOOLEAN")
                )
        );

        // ================================================
        // 6) DAC / MIXER / SOUNDCARD
        // ================================================
        createCategory(
                "DAC / Mixer / Soundcard",
                List.of(
                        att("dacChipset", "Chip DAC", "STRING"),
                        att("sampleRate", "Tần mẫu", "STRING"),
                        att("bitDepth", "Độ sâu bit", "STRING"),
                        att("balancedOutput", "Output cân bằng (XLR)", "BOOLEAN"),
                        att("inputInterface", "Cổng input", "STRING"),
                        att("outputInterface", "Cổng output", "STRING"),
                        att("channelCount", "Số kênh", "NUMBER"),
                        att("hasPhantomPower", "Nguồn phantom (+48V)", "BOOLEAN"),
                        att("eqBands", "Dải EQ", "STRING"),
                        att("faderType", "Loại fader", "STRING")
                )
        );

        log.info("🎉 Default Categories initialized successfully!");
    }

    // ================================================
    // HELPERS
    // ================================================

    private CategoryAttribute att(String name, String label, String type) {
        return CategoryAttribute.builder()
                .attributeName(name)
                .attributeLabel(label)
                .dataType(type)
                .build();
    }

    private void createCategory(String name, List<CategoryAttribute> attributes) {

        if (categoryRepo.existsByNameIgnoreCase(name)) {
            log.info("⚠ Category '{}' exists → skip", name);
            return;
        }

        Category cate = Category.builder()
                .name(name)
                .parent(null)
                .build();

        cate = categoryRepo.save(cate);

        for (CategoryAttribute attr : attributes) {
            attr.setCategory(cate);
            attrRepo.save(attr);
        }

        log.info("✅ Category '{}' created with {} attributes", name, attributes.size());
    }
}
