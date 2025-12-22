package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AudioChatService {

    private final OpenAiChatModel chatModel;
    private final AiChatMemoryService memoryService;

    public String chat(String userId, String message) {

        // Save user message
        memoryService.saveUserMessage(userId, message);

        // Load user-specific memory (tạo list mutable)
        List<ChatMessage> history = new java.util.ArrayList<>(memoryService.loadMemory(userId));

        // Thêm system định hướng AI ở đầu
        history.add(0, SystemMessage.from("""
            You are an AUDIO CONSULTING ASSISTANT.
            You only answer questions related to:
            - speakers, amplifiers, DAC, subwoofer
            - home cinema, karaoke, hi-fi audio
            - pairing, matching, room setup
            Do NOT answer outside the audio domain.
        """));

        // Kiểm tra xem có thông tin sản phẩm trong bộ nhớ không
        List<ChatMessage> productInfo = history.stream()
            .filter(msg -> msg instanceof AiMessage && msg.toString().contains("productId"))
            .toList();

        if (!productInfo.isEmpty()) {
            // Thêm thông tin sản phẩm vào lịch sử để AI sử dụng
            history.add(1, SystemMessage.from("Thông tin sản phẩm đã lưu: \n" + productInfo.get(productInfo.size() - 1).toString()));
        }

        var response = chatModel.generate(history);
        String reply = response.content().text();

        memoryService.saveAssistantMessage(userId, reply);

        return reply;
    }

    public void saveProductInfo(String userId, Map<String, Object> productData) {
        String sessionKey = AiChatMemoryService.SESSION_CHAT_HISTORY_PREFIX + userId;
        List<ChatMessage> history = (List<ChatMessage>) memoryService.getSession().getAttribute(sessionKey);

        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(SystemMessage.from("Thông tin sản phẩm: \n" + productData.toString()));
        memoryService.getSession().setAttribute(sessionKey, history);
    }
}
