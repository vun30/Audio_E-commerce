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
                description TEXT,
                icon_url VARCHAR(255),
                name VARCHAR(255) NOT NULL UNIQUE,    -- 🔹 Tên danh mục sản phẩm (ví dụ: "Loa", "Tai Nghe", "Micro", ...)
                slug VARCHAR(255),
                sort_order INT
            );
            
            CREATE TABLE products (
                product_id CHAR(36) PRIMARY KEY,
                store_id CHAR(36) NOT NULL,
                category_id CHAR(36) NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(category_id),
            
                -- 🏷️ THÔNG TIN CHUNG
                name VARCHAR(255),                    -- 🔹 Tên sản phẩm
                brand_name VARCHAR(255) NOT NULL,     -- ⚠️ TÊN THƯƠNG HIỆU (người dùng nói "hãng", "brand", "hãng sản xuất" → dùng cột này, KHÔNG có cột 'brand')
                slug VARCHAR(255),
                short_description VARCHAR(255),       -- 🔹 Mô tả ngắn
                description LONGTEXT,                 -- 🔹 Mô tả chi tiết
                model VARCHAR(255),                   -- 🔹 Mã model
                color VARCHAR(255),                   -- 🔹 Màu sắc
                material VARCHAR(255),                -- 🔹 Chất liệu
                dimensions VARCHAR(255),              -- 🔹 Kích thước
                weight DECIMAL(38,2),                 -- 🔹 Trọng lượng (kg hoặc g)
            
                -- 💰 GIÁ & TỒN KHO
                price DECIMAL(38,2) NOT NULL,         -- ⚠️ GIÁ GỐC (user nói “giá”, “price”, “giá bán” → dùng cột này)
            
                -- 📊 TRẠNG THÁI & ĐÁNH GIÁ
                status ENUM('ACTIVE','BANNED','DELETED','DISCONTINUED','DRAFT','INACTIVE','OUT_OF_STOCK','SUSPENDED','UNLISTED'),
                is_featured BIT(1),
                rating_average DECIMAL(38,2),         -- ⚠️ ĐIỂM ĐÁNH GIÁ TRUNG BÌNH (user nói “rating”, “điểm”, “đánh giá” → dùng cột này)
                review_count INT,                     -- 🔹 Số lượng đánh giá
                view_count INT,                       -- 🔹 Lượt xem sản phẩm
            
                -- ⚙️ THÔNG SỐ KỸ THUẬT
                frequency_response VARCHAR(255),
                sensitivity VARCHAR(255),
                impedance VARCHAR(255),
                power_handling VARCHAR(255),
                connection_type VARCHAR(255),
                voltage_input VARCHAR(255),
                warranty_period VARCHAR(255),         -- 🔹 Thời hạn bảo hành
                warranty_type VARCHAR(255),
                manufacturer_name VARCHAR(255),       -- ⚠️ TÊN HÃNG SẢN XUẤT (đừng nhầm với brand_name)
                manufacturer_address VARCHAR(255),
                product_condition VARCHAR(255),       -- 🔹 Tình trạng (Mới, Cũ, Refurbished, ...)
                is_custom_made BIT(1),
            
                -- 🔊 LOA (SPEAKER)
                driver_configuration VARCHAR(255),
                driver_size VARCHAR(255),
                enclosure_type VARCHAR(255),
                coverage_pattern VARCHAR(255),
                crossover_frequency VARCHAR(255),
                placement_type VARCHAR(255),
            
                -- 🎧 TAI NGHE (HEADPHONE)
                headphone_type VARCHAR(255),
                compatible_devices VARCHAR(255),
                is_sports_model BIT(1),
                headphone_features VARCHAR(255),
                battery_capacity VARCHAR(255),
                has_built_in_battery BIT(1),
                is_gaming_headset BIT(1),
                headphone_accessory_type VARCHAR(255),
                headphone_connection_type VARCHAR(255),
                plug_type VARCHAR(255),
                sirim_approved BIT(1),
                sirim_certified BIT(1),
                mcmc_approved BIT(1),
            
                -- 🎤 MICRO
                mic_type VARCHAR(255),
                polar_pattern VARCHAR(255),
                maxspl VARCHAR(255),
                mic_output_impedance VARCHAR(255),
                mic_sensitivity VARCHAR(255),
            
                -- 📻 AMPLI / RECEIVER
                amplifier_type VARCHAR(255),
                total_power_output VARCHAR(255),
                thd VARCHAR(255),
                snr VARCHAR(255),
                input_channels INT,
                output_channels INT,
                support_bluetooth BIT(1),
                support_wifi BIT(1),
                support_airplay BIT(1),
            
                -- 📀 TURNTABLE
                platter_material VARCHAR(255),
                motor_type VARCHAR(255),
                tonearm_type VARCHAR(255),
                auto_return BIT(1),
            
                -- 🎛️ DAC / MIXER / SOUND CARD
                dac_chipset VARCHAR(255),
                sample_rate VARCHAR(255),
                bit_depth VARCHAR(255),
                balanced_output BIT(1),
                input_interface VARCHAR(255),
                output_interface VARCHAR(255),
                channel_count INT,
                has_phantom_power BIT(1),
                eq_bands VARCHAR(255),
                fader_type VARCHAR(255),
                built_in_effects BIT(1),
                usb_audio_interface BIT(1),
                midi_support BIT(1),
            
                -- 🧩 CÁC CỘT PHỤ
                video_url VARCHAR(255)
            );
            
            -- ⚙️ Ghi chú danh mục thường gặp:
            -- "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp", "Turntable", "Sound Card", "DJ Controller", "Combo"
            
            -- ⚠️ LƯU Ý ĐẶC BIỆT CHO AI KHI SINH SQL:
            -- - KHÔNG tự tạo thêm tên cột mới. Chỉ được phép dùng các tên cột đã có trong 2 bảng trên.
            -- - Nếu câu hỏi của người dùng chứa từ khóa lạ, hãy tìm cột tương đương gần nghĩa nhất trong schema này.
            -- - Nếu không có cột tương ứng, trả về lỗi hoặc câu SQL trống, KHÔNG tự bịa ra cột mới.
            --
            -- 🔍 QUY TẮC ÁNH XẠ TỪ KHÓA → CỘT TƯƠNG ỨNG:
            --   "brand" / "hãng" / "thương hiệu"         → products.brand_name
            --   "hãng sản xuất"                          → products.manufacturer_name
            --   "category" / "loại" / "danh mục"         → categories.name
            --   "rating" / "điểm đánh giá" / "đánh giá"  → products.rating_average
            --   "giá" / "price" / "cost" / "giá bán"     → products.price hoặc products.final_price
            --   "số lượng còn" / "tồn kho"               → products.stock_quantity
            --   "trạng thái"                             → products.status
            --   "màu sắc" / "color"                      → products.color
            --   "bảo hành"                               → products.warranty_period
            --   "model"                                  → products.model
            --
            -- 🚫 KHÔNG DÙNG:
            --   - 'brand' (phải dùng brand_name)
            --   - 'categoryName' (phải JOIN categories c ON p.category_id = c.category_id)
            --   - 'rating_star', 'brand_type', 'price_range' hoặc bất kỳ cột không có trong schema
            """;


    // ============================================================
    // 🚀 ADMIN NẠP SCHEMA — GỌI 1 LẦN
    // ============================================================
    @Override
    public String initSchema() {
        try {
            geminiClient.initSchemaGlobal(this.productSchema);

            return """
                    ✅ Product schema (rút gọn) đã nạp toàn cục vào Gemini.
                    ---------------------------------------------
                    📦 Nội dung schema đã gửi:
                    %s
                    ---------------------------------------------
                    """.formatted(this.productSchema);

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
