package org.example.audio_ecommerce.LangChain4J;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

class AiChatMemoryServiceTest {

    @Test
    void saveLastProductInfo_overwrites_and_loads() {
        MockHttpSession session = new MockHttpSession();
        AiChatMemoryService memory = new AiChatMemoryService(session);

        memory.saveLastProductInfo("u1", "p1");
        assertEquals("p1", memory.loadLastProductInfo("u1"));

        memory.saveLastProductInfo("u1", "p2");
        assertEquals("p2", memory.loadLastProductInfo("u1"));
    }

    @Test
    void loadLastProductInfo_returnsNull_whenExpired() {
        MockHttpSession session = new MockHttpSession();
        AiChatMemoryService memory = new AiChatMemoryService(session);

        // set last product and then force expire
        memory.saveLastProductInfo("u1", "p1");
        session.setAttribute("EXPIRES_AT_u1", System.currentTimeMillis() - 1);

        assertNull(memory.loadLastProductInfo("u1"));
    }
}

