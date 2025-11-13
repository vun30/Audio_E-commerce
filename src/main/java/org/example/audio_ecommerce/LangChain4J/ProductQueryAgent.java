package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductQueryAgent {

    private final OpenAiChatModel chatModel;

    public String generateSql(String fullPrompt) {

        var response = chatModel.generate(
                SystemMessage.from("""
                        VERSION: 999999
                        You generate ONLY MySQL SELECT queries.
                        RULES:
                        - No comments
                        - No explanation
                        - No markdown
                        - Only SELECT allowed
                        - Use only schema columns
                        - ALWAYS append: LIMIT 20
                        """),
                UserMessage.from(fullPrompt)
        );

        return response.content().text();

    }
}
