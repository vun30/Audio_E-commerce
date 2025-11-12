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
        CREATE TABLE products (
            product_id CHAR(36) PRIMARY KEY,
            name VARCHAR(255),
            brandName VARCHAR(255),
            categoryName VARCHAR(255),
            price DECIMAL(15,2),
            discountPrice DECIMAL(15,2),
            finalPrice DECIMAL(15,2),
            promotionPercent DECIMAL(5,2),
            stockQuantity INT,
            shippingFee DECIMAL(15,2),
            connectionType VARCHAR(100),
            powerHandling VARCHAR(50),
            driverSize VARCHAR(100),
            impedance VARCHAR(50),
            sensitivity VARCHAR(50),
            frequencyResponse VARCHAR(50),
            amplifierType VARCHAR(50),
            totalPowerOutput VARCHAR(50),
            supportBluetooth BOOLEAN,
            supportWifi BOOLEAN,
            manufacturerName VARCHAR(100),
            warrantyPeriod VARCHAR(50),
            productCondition VARCHAR(50),
            ratingAverage DECIMAL(3,2),
            reviewCount INT,
            viewCount INT,
            isFeatured BOOLEAN,
            status VARCHAR(20)
        );

        -- Các danh mục phổ biến:
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
            Bạn là AI sinh câu lệnh SQL MySQL cho bảng `products` đã được nạp schema.
            ⚙️ Quy tắc:
            - Chỉ chọn sản phẩm có categoryName thuộc ('Tai Nghe', 'Loa', 'Micro', 'DAC', 'Mixer', 'Amp', 
              'Turntable', 'Sound Card', 'DJ Controller', 'Combo').
            - Hãy trả về câu SQL dạng:
              SELECT product_id FROM products WHERE ...
            - Không thêm văn bản, không giải thích.
            👤 Người dùng: %s (%s)
            🧠 Câu hỏi: "%s"
        """.formatted(userName, userId, userMessage);

        String sql;
        try {
            sql = geminiClient.generateSql(userId, prompt).trim();

            // 🧹 Gỡ Markdown nếu có (```sql ... ```)
            if (sql.startsWith("```")) {
                sql = sql.replaceAll("(?s)```(sql)?", "").trim();
            }

            // 🧹 Thay " bằng ' để tránh lỗi MySQL
            sql = sql.replaceAll("\"", "'");

            // 🧹 Xóa dấu ; cuối cùng nếu có
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }

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
    // 💬 CHAT TỰ DO — PHÂN BIỆT USER, CÓ NHỚ CONTEXT
    // ============================================================
    @Override
    public String chatWithGemini(AiQueryRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("❌ Vui lòng nhập câu hỏi hợp lệ.");
        }

        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";
        String userName = request.getUserName() != null ? request.getUserName() : "guest";
        String message = request.getMessage();

        try {
            System.out.printf("💬 [%s] Gửi câu hỏi AI: %s%n", userName, message);
            String answer = geminiClient.chat(userId, message);
            System.out.printf("🤖 [%s] Gemini trả lời: %s%n", userName, answer);
            return answer;
        } catch (Exception e) {
            return "⚠️ Lỗi khi gọi Gemini API cho user " + userName + ": " + e.getMessage();
        }
    }
}
