package org.example.audio_ecommerce.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AiQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 🧠 Thực thi câu SQL do Gemini sinh ra để lấy product_id.
     * - Chỉ cho phép SELECT từ bảng `products`.
     * - Nếu chưa có LIMIT, sẽ tự động thêm LIMIT 50.
     * - Trả về List<UUID> (danh sách product_id).
     */
    public List<UUID> executeQuery(String sql) {
        String lower = sql.toLowerCase();

        // 🔒 Kiểm tra an toàn
        if (!lower.startsWith("select") || !lower.contains("from products")) {
            throw new RuntimeException("❌ SQL không hợp lệ hoặc sai bảng: " + sql);
        }
        if (lower.contains("delete") || lower.contains("update") || lower.contains("drop") || lower.contains("truncate")) {
            throw new RuntimeException("❌ SQL nguy hiểm bị chặn: " + sql);
        }

        // 🔄 Ép buộc LIMIT để tránh query quá lớn
        if (!lower.contains("limit")) {
            sql += " LIMIT 50";
        }

        System.out.println("🧾 Thực thi SQL: " + sql);

        // ✅ Thực thi query và map ra UUID của product_id
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                UUID.fromString(rs.getString("product_id"))
        );
    }
}
