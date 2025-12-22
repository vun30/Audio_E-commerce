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
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessage> loadMemory(String userId) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);

        if (history == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(history); // Trả về bản sao để tránh sửa đổi trực tiếp
    }

    /**
     * Lưu tin nhắn của người dùng vào HttpSession
     */
    public void saveUserMessage(String userId, String content) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);

        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(UserMessage.from(content));
        session.setAttribute(sessionKey, history);
    }

    /**
     * Lưu tin nhắn của AI vào HttpSession
     */
    public void saveAssistantMessage(String userId, String content) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);

        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(AiMessage.from(content));
        session.setAttribute(sessionKey, history);
    }

    /**
     * Lưu summary sản phẩm (role = assistant)
     */
    public void saveSearchSummary(String userId, List<Map<String, Object>> items) {
        String compressed = compressProductSummary(items);
        saveMessage(userId, AiMessage.from(compressed));
    }

    @SuppressWarnings("unchecked")
    private void saveMessage(String userId, ChatMessage message) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);

        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(message);

        // Giới hạn số lượng tin nhắn trong lịch sử (ví dụ: 20 tin nhắn)
        if (history.size() > 20) {
            history = history.subList(history.size() - 20, history.size());
        }

        session.setAttribute(sessionKey, history);
    }

    /**
     * Xóa lịch sử hội thoại sau 10 phút
     */
    public void clearSessionAfterTimeout(String userId) {
        String sessionKey = SESSION_CHAT_HISTORY_PREFIX + userId;
        session.removeAttribute(sessionKey);
    }

    public HttpSession getSession() {
        return session;
    }
}