package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AiQueryRequest;
import org.example.audio_ecommerce.dto.response.AiQueryResponse;
import org.example.audio_ecommerce.repository.AiQueryRepository;
import org.example.audio_ecommerce.service.AiQueryService;
import org.example.audio_ecommerce.util.GeminiClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiQueryServiceImpl implements AiQueryService {

    private final GeminiClient geminiClient;
    private final AiQueryRepository aiQueryRepository;

    // ============================================================
    // 🔧 SCHEMA sản phẩm rút gọn
    // ============================================================
    private final String productSchema = """
            CREATE TABLE categories (
                category_id CHAR(36) PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,     -- Tên danh mục: Loa, Micro, DAC, Mixer, Amp, ...
                slug VARCHAR(255),
                description TEXT,
                icon_url VARCHAR(255),
                sort_order INT
            );
            
            CREATE TABLE products (
                product_id CHAR(36) PRIMARY KEY,
                store_id CHAR(36) NOT NULL,
                category_id CHAR(36) NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(category_id),
            
                -- 🏷️ Thông tin chung
                name VARCHAR(255),
                brand_name VARCHAR(255) NOT NULL,
                slug VARCHAR(255),
                short_description TEXT,
                description LONGTEXT,
                model VARCHAR(100),
                color VARCHAR(100),
                material VARCHAR(100),
                dimensions VARCHAR(100),
                weight DECIMAL(10,2),
            
                -- 💰 Giá & tồn kho
                sku VARCHAR(100),
                price DECIMAL(15,2) NOT NULL,
                discount_price DECIMAL(15,2),
                promotion_percent DECIMAL(5,2),
                price_after_promotion DECIMAL(15,2),
                price_before_voucher DECIMAL(15,2),
                voucher_amount DECIMAL(15,2),
                final_price DECIMAL(15,2),
                platform_fee_percent DECIMAL(5,2),
                currency VARCHAR(10),
                stock_quantity INT,
                warehouse_location VARCHAR(255),
            
                -- 🚚 Vận chuyển
                shipping_fee DECIMAL(15,2),
                province_code VARCHAR(10),
                district_code VARCHAR(10),
                ward_code VARCHAR(10),
                shipping_address VARCHAR(255),
            
                -- 📊 Trạng thái & đánh giá
                status VARCHAR(20),
                is_featured TINYINT(1),
                rating_average DECIMAL(3,2),
                review_count INT,
                view_count INT,
            
                -- 🕒 Thời gian
                created_at DATETIME,
                updated_at DATETIME,
                last_updated_at DATETIME,
                last_update_interval_days BIGINT,
                created_by CHAR(36),
                updated_by CHAR(36),
            
                -- ⚙️ Thông số kỹ thuật
                frequency_response VARCHAR(100),
                sensitivity VARCHAR(100),
                impedance VARCHAR(50),
                power_handling VARCHAR(50),
                connection_type VARCHAR(100),
                voltage_input VARCHAR(50),
                warranty_period VARCHAR(50),
                warranty_type VARCHAR(100),
                manufacturer_name VARCHAR(100),
                manufacturer_address VARCHAR(255),
                product_condition VARCHAR(50),
                is_custom_made TINYINT(1),
            
                -- 🔊 Loa (Speaker)
                driver_configuration VARCHAR(100),
                driver_size VARCHAR(100),
                enclosure_type VARCHAR(100),
                coverage_pattern VARCHAR(100),
                crossover_frequency VARCHAR(100),
                placement_type VARCHAR(100),
            
                -- 🎧 Tai nghe (Headphone)
                headphone_type VARCHAR(100),
                compatible_devices VARCHAR(255),
                is_sports_model TINYINT(1),
                headphone_features VARCHAR(255),
                battery_capacity VARCHAR(50),
                has_built_in_battery TINYINT(1),
                is_gaming_headset TINYINT(1),
                headphone_accessory_type VARCHAR(100),
                headphone_connection_type VARCHAR(100),
                plug_type VARCHAR(100),
                sirim_approved TINYINT(1),
                sirim_certified TINYINT(1),
                mcmc_approved TINYINT(1),
            
                -- 🎤 Micro
                mic_type VARCHAR(100),
                polar_pattern VARCHAR(100),
                max_spl VARCHAR(50),
                mic_output_impedance VARCHAR(50),
                mic_sensitivity VARCHAR(50),
            
                -- 📻 Ampli / Receiver
                amplifier_type VARCHAR(100),
                total_power_output VARCHAR(100),
                thd VARCHAR(50),
                snr VARCHAR(50),
                input_channels INT,
                output_channels INT,
                support_bluetooth TINYINT(1),
                support_wifi TINYINT(1),
                support_airplay TINYINT(1),
            
                -- 📀 Turntable
                platter_material VARCHAR(100),
                motor_type VARCHAR(100),
                tonearm_type VARCHAR(100),
                auto_return TINYINT(1),
            
                -- 🎛️ DAC / Mixer / Sound Card
                dac_chipset VARCHAR(100),
                sample_rate VARCHAR(100),
                bit_depth VARCHAR(50),
                balanced_output TINYINT(1),
                input_interface VARCHAR(255),
                output_interface VARCHAR(255),
                channel_count INT,
                has_phantom_power TINYINT(1),
                eq_bands VARCHAR(100),
                fader_type VARCHAR(100),
                built_in_effects TINYINT(1),
                usb_audio_interface TINYINT(1),
                midi_support TINYINT(1)
            );
            
            -- ⚙️ Các danh mục phổ biến (categories.name):
            -- "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp",
            -- "Turntable", "Sound Card", "DJ Controller", "Combo"
            """;


    // ============================================================
    // 🚀 ADMIN NẠP SCHEMA — GỌI 1 LẦN
    // ============================================================
    @Override
    public String initSchema() {
        try {
            geminiClient.initSchemaGlobal(this.productSchema);
            return "✅ Product schema (rút gọn) đã nạp toàn cục vào Gemini.";
        } catch (Exception e) {
            return "⚠️ Lỗi khi nạp schema: " + e.getMessage();
        }
    }

    // ============================================================
    // 💬 SINH SQL TỪ PROMPT — PHÂN BIỆT USER
    // ============================================================
    @Override
    public AiQueryResponse handleUserQuery(AiQueryRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("❌ Vui lòng nhập câu hỏi hợp lệ.");
        }

        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";
        String userName = request.getUserName() != null ? request.getUserName() : "guest";
        String userMessage = request.getMessage();

        String prompt = """
                Bạn là AI chuyên phân tích dữ liệu MySQL cho nền tảng TMĐT thiết bị âm thanh.
                Dữ liệu có 2 bảng:
                  - `products`: chứa thông tin sản phẩm (giá, thương hiệu, rating, v.v.)
                  - `categories`: chứa danh mục, liên kết qua `products.category_id`.
                
                Quy tắc sinh SQL:
                  1. Luôn JOIN bảng `categories` khi lọc theo danh mục.
                     👉 Ví dụ: JOIN categories c ON p.category_id = c.category_id
                  2. Lọc danh mục bằng `c.name` (vd: WHERE c.name = 'Loa')
                  3. Không bao giờ dùng `categoryName` trong bảng products.
                  4. Chỉ sinh SELECT — không UPDATE, DELETE, DROP, INSERT.
                  5. Giới hạn kết quả bằng LIMIT 50 nếu user không nêu rõ.
                  6. Trả về cú pháp MySQL hợp lệ duy nhất — không có mô tả hay markdown.
                
                Câu hỏi người dùng:
                👤 %s (%s)
                💬 "%s"
                
                Trả về đúng 1 câu SQL duy nhất.
                """.formatted(userName, userId, userMessage);

        String sql;
        try {
            sql = geminiClient.generateSql(prompt).trim();
            if (sql.startsWith("```")) {
                sql = sql.replaceAll("(?s)```(sql)?", "").trim();
            }
            sql = sql.replaceAll("\"", "'");
            if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1);
        } catch (Exception ex) {
            throw new RuntimeException("⚠️ Lỗi khi gọi Gemini SQL API: " + ex.getMessage());
        }

        System.out.printf("🤖 [%s] Gemini sinh SQL: %s%n", userName, sql);

        // 🛡️ Kiểm tra an toàn cơ bản
        String lower = sql.toLowerCase();
        if (!lower.startsWith("select") || !lower.contains("from products"))
            throw new RuntimeException("❌ SQL không hợp lệ hoặc sai bảng: " + sql);
        if (lower.contains("delete") || lower.contains("update") || lower.contains("drop") || lower.contains("truncate"))
            throw new RuntimeException("❌ SQL nguy hiểm bị chặn: " + sql);

        // ✅ Thực thi SQL
        List<UUID> productIds = aiQueryRepository.executeQuery(sql);

        // ✅ Trả kết quả
        return AiQueryResponse.builder()
                .generatedSql(sql)
                .rows(
                        productIds.stream()
                                .map(id -> Map.<String, Object>of("product_id", id.toString()))
                                .toList()
                )
                .build();
    }

    // ============================================================
    // 🎧 API /chat → chỉ hội thoại chủ đề âm thanh (không đọc DB)
    // ============================================================
    @Override
    public String chatWithGemini(AiQueryRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("❌ Vui lòng nhập câu hỏi hợp lệ.");
        }

        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";
        String userName = request.getUserName() != null ? request.getUserName() : "guest";
        String message = request.getMessage();

        String topicPrompt = """
                Bạn là chuyên gia trong lĩnh vực Âm thanh, Thiết bị Audio và Điện tử âm thanh.
                
                Quy tắc:
                - Trả lời các câu hỏi về loa, tai nghe, DAC, ampli, micro, mixer, nhạc số, kỹ thuật nghe nhạc, phòng nghe, thiết bị thu âm, v.v.
                - Nếu câu hỏi không liên quan đến âm thanh hoặc thiết bị audio, hãy từ chối nhẹ nhàng:
                  "Xin lỗi, tôi chỉ hỗ trợ các chủ đề liên quan đến âm thanh và thiết bị audio."
                - Trả lời bằng tiếng Việt, tự nhiên, chính xác và thân thiện.
                
                Câu hỏi từ người dùng:
                👤 %s (%s)
                💬 "%s"
                """.formatted(userName, userId, message);
        try {
            String answer = geminiClient.chat(userId, topicPrompt);
            System.out.printf("🎙️ [%s] Gemini (Audio Expert): %s%n", userName, answer);
            return answer;
        } catch (Exception e) {
            return "⚠️ Lỗi khi gọi Gemini API: " + e.getMessage();
        }

    }
}
