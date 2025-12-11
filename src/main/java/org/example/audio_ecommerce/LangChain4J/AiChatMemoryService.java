package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatMemoryService {

    private final AiChatHistoryRepository repo;

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
    public List<ChatMessage> loadMemory(String userId) {

        List<AiChatHistory> records =
                repo.findTop20ByUserIdOrderByCreatedAtDesc(userId);

        // Đảo thứ tự từ CŨ -> MỚI
        Collections.reverse(records);

        return records.stream()
                .map(h -> {
                    if ("user".equals(h.getRole())) {
                        return UserMessage.from(h.getContent());
                    } else {
                        return AiMessage.from(h.getContent());
                    }
                })
                .toList();
    }

    /**
     * Lưu tin nhắn từ user
     */
    public void saveUserMessage(String userId, String content) {
        repo.save(new AiChatHistory(null, userId, "user", content, LocalDateTime.now()));
    }

    /**
     * Lưu tin nhắn từ assistant
     */
    public void saveAssistantMessage(String userId, String content) {
        repo.save(new AiChatHistory(null, userId, "assistant", content, LocalDateTime.now()));
    }

    /**
     * Lưu summary sản phẩm (role = assistant)
     */
    public void saveSearchSummary(String userId, List<Map<String, Object>> items) {

        String compressed = compressProductSummary(items);

        repo.save(new AiChatHistory(
                null,
                userId,
                "assistant",
                compressed,
                LocalDateTime.now()
        ));
    }
}
