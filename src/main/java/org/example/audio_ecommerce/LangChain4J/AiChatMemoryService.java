package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatMemoryService {

    private final HttpSession session;

    public static final String SESSION_CHAT_HISTORY_PREFIX = "CHAT_HISTORY_";
    public static final String SESSION_LAST_PRODUCT_PREFIX = "LAST_PRODUCT_";
    private static final String SESSION_EXPIRES_AT_PREFIX = "EXPIRES_AT_";

    private static final long DEFAULT_TTL_MILLIS = 10 * 60 * 1000L; // 10 minutes

    private static final String USER_PREFIX = "U:";
    private static final String AI_PREFIX = "A:";

    /**
     * Nén danh sách sản phẩm để AI nhớ nhưng không tốn token
     */
    public String compressProductSummary(List<Map<String, Object>> items) {

        StringBuilder sb = new StringBuilder();
        sb.append("SEARCH_RESULT:\n");

        int index = 1;
        for (Map<String, Object> item : items) {
            sb.append(index++)
              .append(". ")
              .append(item.get("brand")).append(" ")
              .append(item.get("name"))
              .append(" | price=").append(item.get("effectivePrice"))
              .append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Load 20 tin nhắn gần nhất theo đúng thứ tự cũ -> mới
     * NOTE: session JDBC chỉ serialize được object đơn giản/Serializable.
     * Vì vậy ta lưu lịch sử dưới dạng List<String> và convert lại khi load.
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessage> loadMemory(String userId) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<String> raw = (List<String>) session.getAttribute(sessionKey);

        if (raw == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> history = new ArrayList<>();
        for (String line : raw) {
            if (line == null) continue;
            if (line.startsWith(USER_PREFIX)) {
                history.add(UserMessage.from(line.substring(USER_PREFIX.length()).trim()));
            } else if (line.startsWith(AI_PREFIX)) {
                history.add(AiMessage.from(line.substring(AI_PREFIX.length()).trim()));
            }
        }
        return history;
    }

    /**
     * Lưu tin nhắn của người dùng vào HttpSession (store as String)
     */
    @SuppressWarnings("unchecked")
    public void saveUserMessage(String userId, String content) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<String> raw = (List<String>) session.getAttribute(sessionKey);

        if (raw == null) {
            raw = new ArrayList<>();
        }

        raw.add(USER_PREFIX + " " + content);

        if (raw.size() > 20) {
            raw = raw.subList(raw.size() - 20, raw.size());
        }

        session.setAttribute(sessionKey, raw);
    }

    /**
     * Lưu tin nhắn của AI vào HttpSession (store as String)
     */
    @SuppressWarnings("unchecked")
    public void saveAssistantMessage(String userId, String content) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<String> raw = (List<String>) session.getAttribute(sessionKey);

        if (raw == null) {
            raw = new ArrayList<>();
        }

        raw.add(AI_PREFIX + " " + content);

        if (raw.size() > 20) {
            raw = raw.subList(raw.size() - 20, raw.size());
        }

        session.setAttribute(sessionKey, raw);
    }

    /**
     * Deprecated: giữ lại để tương thích, nhưng giờ đã không còn lưu ChatMessage object.
     */
    public void saveSearchSummary(String userId, List<Map<String, Object>> items) {
        String compressed = compressProductSummary(items);
        saveAssistantMessage(userId, compressed);
    }

    /**
     * Lưu thông tin sản phẩm gần nhất để AI chỉ tư vấn dựa trên product vừa nạp.
     * Không trộn vào chat history để tránh "tìm theo ngữ cảnh".
     */
    public void saveLastProductInfo(String userId, String content) {
        String key = SESSION_LAST_PRODUCT_PREFIX + userId;
        session.setAttribute(key, content);
        touchTtl(userId);
    }

    /**
     * Load thông tin sản phẩm gần nhất (nếu chưa hết hạn TTL).
     */
    public String loadLastProductInfo(String userId) {
        if (isExpired(userId)) {
            clearAll(userId);
            return null;
        }
        String key = SESSION_LAST_PRODUCT_PREFIX + userId;
        Object v = session.getAttribute(key);
        return v == null ? null : v.toString();
    }

    /**
     * Chạm TTL: mỗi khi có tương tác (chat hoặc nạp sản phẩm) thì gia hạn thêm 10 phút.
     */
    public void touchTtl(String userId) {
        session.setAttribute(SESSION_EXPIRES_AT_PREFIX + userId, System.currentTimeMillis() + DEFAULT_TTL_MILLIS);
    }

    public boolean isExpired(String userId) {
        Object expiresAt = session.getAttribute(SESSION_EXPIRES_AT_PREFIX + userId);
        if (expiresAt == null) return false; // chưa set TTL thì coi như chưa hết hạn

        long ts;
        if (expiresAt instanceof Long l) {
            ts = l;
        } else {
            try {
                ts = Long.parseLong(expiresAt.toString());
            } catch (Exception e) {
                return false;
            }
        }
        return System.currentTimeMillis() > ts;
    }

    /**
     * Xóa toàn bộ bộ nhớ của user (chat + last product).
     */
    public void clearAll(String userId) {
        session.removeAttribute(SESSION_CHAT_HISTORY_PREFIX + userId);
        session.removeAttribute(SESSION_LAST_PRODUCT_PREFIX + userId);
        session.removeAttribute(SESSION_EXPIRES_AT_PREFIX + userId);
    }

    /**
     * Giữ lại method cũ để tương thích nhưng đổi nghĩa thành "gia hạn TTL".
     * Trước đây method này xóa ngay lập tức khiến deploy không thể nhớ.
     */
    public void clearSessionAfterTimeout(String userId) {
        touchTtl(userId);
    }

    public HttpSession getSession() {
        return session;
    }
}

