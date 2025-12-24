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
        memoryService.touchTtl(userId);

        // Load user-specific memory (tạo list mutable)
        List<ChatMessage> history = new java.util.ArrayList<>(memoryService.loadMemory(userId));

        // System rules
        history.add(0, SystemMessage.from("""
            You are an AUDIO CONSULTING ASSISTANT.
            You only answer questions related to:
            - speakers, amplifiers, DAC, subwoofer
            - home cinema, karaoke, hi-fi audio
            - pairing, matching, room setup
            Do NOT answer outside the audio domain.

            PRODUCT RULE (STRICT):
            - If product information is provided below as "LAST_PRODUCT", you MUST ONLY use that product.
            - Do NOT use older chat history to infer other products.
            - If there is no LAST_PRODUCT and the user asks about "the product", ask them to provide a productId or send product information first.
        """));

        // Inject ONLY last product info (not scanning full history)
        String lastProduct = memoryService.loadLastProductInfo(userId);
        if (lastProduct != null && !lastProduct.isBlank()) {
            history.add(1, SystemMessage.from("LAST_PRODUCT:\n" + lastProduct));
        }

        var response = chatModel.generate(history);
        String reply = response.content().text();

        memoryService.saveAssistantMessage(userId, reply);
        memoryService.touchTtl(userId);

        return reply;
    }

    /**
     * Deprecated: không dùng nữa. Giữ lại để tránh breaking nhưng chuyển sang saveLastProductInfo.
     */
    @Deprecated
    public void saveProductInfo(String userId, Map<String, Object> productData) {
        memoryService.saveLastProductInfo(userId, productData == null ? null : productData.toString());
    }
}
