package org.example.audio_ecommerce.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String API_KEY;

    @Value("${gemini.model:gemini-2.0-flash-lite}")
    private String MODEL;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private final HttpClient http = HttpClient.newHttpClient();

    // ============================================================
    // 🧠 Bộ nhớ per-user & schema toàn cục
    // ============================================================
    private static class SessionData {
        StringBuilder history = new StringBuilder();
        long lastActive = Instant.now().toEpochMilli();
    }

    private final Map<String, SessionData> userConversations = new ConcurrentHashMap<>();
    private volatile String globalSchemaSession;

    // ============================================================
    // 🚀 ADMIN KHỞI TẠO SCHEMA 1 LẦN (TOÀN CỤC)
    // ============================================================
    public synchronized void initSchemaGlobal(String schema) {
        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("❌ Schema rỗng, không thể gửi lên Gemini.");
        }

        try {
            sendSchema(schema);
            this.globalSchemaSession = "gemini-global-schema-" + System.currentTimeMillis();
            System.out.println("✅ Schema toàn cục đã được nạp thành công.");
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi khi nạp schema: " + e.getMessage(), e);
        }
    }

    private void sendSchema(String schema) throws Exception {
        String body = """
        {
          "contents": [{
            "role": "user",
            "parts": [{
              "text": "Hãy ghi nhớ cấu trúc bảng Product sau đây để sử dụng cho các câu SQL tiếp theo. KHÔNG cần phản hồi gì thêm.\\n\\n%s"
            }]
          }]
        }
        """.formatted(schema.replace("\"", "\\\""));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/models/" + MODEL + ":generateContent?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Gemini Error (init): " + resp.body());
        }
    }

    // ============================================================
    // 💬 CHAT TỰ DO — NHỚ THEO USER
    // ============================================================
    public String chat(String userId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("❌ Câu hỏi không hợp lệ hoặc trống.");
        }
        if (userId == null || userId.isBlank()) userId = "guest";

        userConversations.putIfAbsent(userId, new SessionData());
        SessionData session = userConversations.get(userId);
        session.lastActive = Instant.now().toEpochMilli();

        // Giới hạn dung lượng để tránh tràn
        if (session.history.length() > 4000) {
            session.history.delete(0, session.history.length() - 2000);
        }

        session.history.append("User: ").append(message).append("\nAI: ");

        String prompt = """
            Bạn là trợ lý AI thân thiện, trả lời tự nhiên bằng tiếng Việt.
            Dưới đây là hội thoại trước đó (chỉ trả lời câu hỏi cuối cùng, không nhắc lại lịch sử):

            %s
        """.formatted(session.history);

        try {
            String body = """
            {
              "contents": [{
                "role": "user",
                "parts": [{ "text": "%s" }]
              }]
            }
            """.formatted(prompt.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/models/" + MODEL + ":generateContent?key=" + API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Gemini Error (chat): " + response.body());

            JSONObject json = new JSONObject(response.body());
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty())
                throw new RuntimeException("❌ Không có phản hồi hợp lệ từ Gemini.");

            JSONObject first = candidates.getJSONObject(0);
            JSONObject content = first.optJSONObject("content");
            JSONArray parts = (content != null) ? content.optJSONArray("parts") : null;
            if (parts == null || parts.isEmpty())
                throw new RuntimeException("❌ Không có phần text trả về.");

            String result = parts.getJSONObject(0).optString("text", "").trim();
            session.history.append(result).append("\n");

            System.out.printf("💬 [User %s] Gemini trả lời: %s%n", userId, result);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Gemini API Error (chat): " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 🧠 GENERATE SQL — DÙNG SCHEMA TOÀN CỤC + USER SESSION
    // ============================================================
    public String generateSql(String userId, String prompt) {
        if (userId == null || userId.isBlank()) userId = "guest";

        if (globalSchemaSession == null) {
            throw new RuntimeException("⚠️ Chưa có schema toàn cục. Admin cần gọi /init-schema trước.");
        }

        userConversations.putIfAbsent(userId, new SessionData());
        SessionData session = userConversations.get(userId);
        session.lastActive = Instant.now().toEpochMilli();

        session.history.append("User (SQL Request): ").append(prompt).append("\nAI (SQL): ");

        String fullPrompt = """
            Bạn là AI sinh câu lệnh SQL MySQL cho bảng `products` (schema đã được nạp).
            Hãy chỉ trả về câu SQL hợp lệ, không thêm giải thích.
            ---
            %s
        """.formatted(prompt);

        try {
            String body = """
            {
              "contents": [{
                "role": "user",
                "parts": [{ "text": "%s" }]
              }]
            }
            """.formatted(fullPrompt.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/models/" + MODEL + ":generateContent?key=" + API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Gemini Error (SQL): " + response.body());

            JSONObject json = new JSONObject(response.body());
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty())
                throw new RuntimeException("❌ Không có phản hồi hợp lệ từ Gemini.");

            JSONObject first = candidates.getJSONObject(0);
            JSONObject content = first.optJSONObject("content");
            JSONArray parts = (content != null) ? content.optJSONArray("parts") : null;
            if (parts == null || parts.isEmpty())
                throw new RuntimeException("❌ Không có phần text trả về.");

            String result = parts.getJSONObject(0).optString("text", "").trim();
            session.history.append(result).append("\n");

            System.out.printf("🤖 [User %s] SQL trả về: %s%n", userId, result);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Gemini API Error (SQL): " + e.getMessage(), e);
        }
    }

    // ============================================================
    // ✅ TỰ ĐỘNG DỌN SESSION IDLE > 10 PHÚT
    // ============================================================
    @Scheduled(fixedRate = 300000) // mỗi 5 phút chạy 1 lần
    public void cleanupInactiveSessions() {
        long now = Instant.now().toEpochMilli();
        long timeout = 10 * 60 * 1000; // 10 phút
        int before = userConversations.size();

        userConversations.entrySet().removeIf(entry -> now - entry.getValue().lastActive > timeout);

        int after = userConversations.size();
        if (before != after) {
            System.out.printf("🧹 Đã dọn session cũ (trước: %d → sau: %d)%n", before, after);
        }
    }

    // ============================================================
    // ✅ Kiểm tra & clear thủ công
    // ============================================================
    public boolean hasGlobalSchema() {
        return globalSchemaSession != null;
    }

    public boolean hasUserSession(String userId) {
        return userConversations.containsKey(userId);
    }

    public void clearUserMemory(String userId) {
        userConversations.remove(userId);
        System.out.println("🧹 Đã xóa hội thoại cho user: " + userId);
    }

    public void clearAllSessions() {
        userConversations.clear();
        System.out.println("🧼 Đã xóa toàn bộ session của mọi user");
    }
}
