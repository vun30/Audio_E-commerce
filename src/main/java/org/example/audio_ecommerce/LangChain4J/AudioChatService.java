package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        var response = chatModel.generate(history);
        String reply = response.content().text();

        memoryService.saveAssistantMessage(userId, reply);

        return reply;
    }
}

