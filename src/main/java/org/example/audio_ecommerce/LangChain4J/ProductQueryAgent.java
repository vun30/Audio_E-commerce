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
                        - Always output a single valid MySQL statement.
                        PRICE LOGIC:
                        When the user refers to price, budget, cost, cheaper, expensive or any numeric range related to pricing:
                        1. Treat the effective price of a product as the COALESCE of (variant_price, final_price, discount_price, price).
                        2. This means you MUST LEFT JOIN product_variants (alias pv) to products (alias p) when price filtering is requested.
                        3. Use expression: COALESCE(pv.variant_price, p.final_price, p.discount_price, p.price)
                        4. Always select DISTINCT p.product_id to avoid duplicates when there are multiple variants.
                        5. If ordering by price, order by the same COALESCE expression.
                        6. If filtering by a range, apply it to the COALESCE expression.
                        GENERAL GUIDELINES:
                        - Alias products as p when joining.
                        - Only use columns present in the provided schema.
                        - If the question asks for limits like top N cheaper or most expensive, still append LIMIT 20 but also ORDER BY price ascending/descending accordingly.
                        - Never use UPDATE/DELETE/INSERT; only SELECT.
                        - If no price-related terms are in the question, you may omit the variant join unless variant attributes are explicitly referenced.
                        OUTPUT:
                        - Return only the SQL without surrounding backticks.
                        - Ensure it ends with LIMIT 20.
                        """),
                UserMessage.from(fullPrompt)
        );

        return response.content().text();

    }
}
