package org.example.audio_ecommerce.LangChain4J;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductAiQueryService {

    private final ProductQueryAgent agent;
    private final SqlExecutorTool sqlExecutor;
    private final SchemaLoader schemaLoader;

    public List<UUID> searchProduct(String naturalQuestion) {

        String fullPrompt =
                "User question: " + naturalQuestion + "\n\n" +
                "SCHEMA:\n" + schemaLoader.loadSchema();

        System.out.println("===== FULL PROMPT =====");
        System.out.println(fullPrompt);
        System.out.println("========================");

        String sql = agent.generateSql(fullPrompt);

        System.out.println("===== SQL GENERATED =====");
        System.out.println(sql);
        System.out.println("=========================");

        List<Map<String, Object>> rows = sqlExecutor.runSelect(sql);

        return rows.stream()
                .map(r -> UUID.fromString(r.get("product_id").toString()))
                .toList();
    }
}
