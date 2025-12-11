package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatMemoryService {

    private final AiChatHistoryRepository repo;

    public List<ChatMessage> loadMemory(String userId) {

        List<AiChatHistory> records = repo.findTop20ByUserIdOrderByCreatedAtDesc(userId);

        // Đảo lại cho thành cũ -> mới
        java.util.Collections.reverse(records);

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

    public void saveUserMessage(String userId, String content) {
        repo.save(new AiChatHistory(null, userId, "user", content, LocalDateTime.now()));
    }

    public void saveAssistantMessage(String userId, String content) {
        repo.save(new AiChatHistory(null, userId, "assistant", content, LocalDateTime.now()));
    }
}