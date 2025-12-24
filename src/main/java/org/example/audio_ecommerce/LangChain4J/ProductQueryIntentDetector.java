package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductQueryIntentDetector {

    private final OpenAiChatModel chatModel;

    public String detectIntent(String userMessage) {

        var response = chatModel.generate(
                SystemMessage.from("""
                        You are an INTENT CLASSIFIER for an AUDIO ASSISTANT.
                        Always output ONLY ONE of the following labels:

                        ADVICE
                        NONE

                        ===========================
                        OUTPUT "ADVICE" IF:
                        ===========================
                        User is asking about audio / speakers / amply / dac / subwoofer or audio setup.
                        This includes questions that previously looked like "SEARCH".

                        ===========================
                        OUTPUT "NONE" IF:
                        ===========================
                        User asks unrelated topics.

                        RULES:
                        - Must output EXACTLY one of: ADVICE / NONE
                        - No explanation.
                        """),
                UserMessage.from(userMessage)
        );

        String intent = response.content().text() == null ? "" : response.content().text().trim().toUpperCase();
        if (!"ADVICE".equals(intent) && !"NONE".equals(intent)) {
            // hard fallback
            return "ADVICE";
        }
        return intent;
    }
}
