package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.ChatMessage;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChatMemoryServiceFormatTest {

    @Test
    void stores_history_as_strings_and_loads_back_as_messages() {
        HttpSession session = new MockHttpSession();
        AiChatMemoryService svc = new AiChatMemoryService(session);

        svc.saveUserMessage("u1", "hi");
        svc.saveAssistantMessage("u1", "hello");

        Object raw = session.getAttribute(AiChatMemoryService.SESSION_CHAT_HISTORY_PREFIX + "u1");
        assertTrue(raw instanceof List);
        assertTrue(((List<?>) raw).get(0) instanceof String);

        List<ChatMessage> history = svc.loadMemory("u1");
        assertEquals(2, history.size());
        assertTrue(history.get(0).toString().contains("hi"));
        assertTrue(history.get(1).toString().contains("hello"));
    }
}

