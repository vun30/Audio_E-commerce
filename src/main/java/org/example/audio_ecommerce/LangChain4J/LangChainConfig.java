//package org.example.audio_ecommerce.LangChain4J;
//
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class LangChainConfig {
//
//    @Value("${gemini.api.key}")
//    private String geminiApiKey;
//
//    @Value("${gemini.model}")
//    private String geminiModel;
//
//    @Bean
//    public OpenAiChatModel chatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(geminiApiKey)
//                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
//                .modelName(geminiModel)      // "gemini-2.5-flash" or "gemini-2.5-flash-lite"
//                .temperature(0.0)
//                .maxTokens(2048)
//                .build();
//    }
//}
package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.base.url}")
    private String baseUrl;

    @Value("${llm.model}")
    private String model;

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)     // mỗi provider có BASE_URL khác nhau
                .modelName(model)     // tên model tuỳ thuộc nhà cung cấp
                .temperature(0.2)
                .maxTokens(2048)
                .build();
    }
}
