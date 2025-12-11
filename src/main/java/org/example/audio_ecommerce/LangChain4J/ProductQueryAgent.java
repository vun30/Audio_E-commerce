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
                    VERSION: 100002
                    You generate ONLY MySQL SELECT queries.

                    ===========================
                    GLOBAL RULES
                    ===========================
                    - No comments
                    - No markdown
                    - No explanation
                    - Only SELECT allowed
                    - Always return ONE SQL query
                    - Always SELECT DISTINCT p.product_id
                    - Always append LIMIT 20

                    Alias:
                        products p
                        product_variants pv
                        product_categories pc
                        categories c
                        category_attributes ca
                        product_attribute_values pav

                    ===========================
                    ALLOWED FILTER TYPES
                    ===========================
                    1. CATEGORY NAME
                    2. BRAND NAME
                    3. PRICE RANGE / BUDGET
                    4. CATEGORY ATTRIBUTE VALUES (fuzzy match)

                    ===========================
                    CATEGORY RULE
                    ===========================
                    JOIN pc + c
                    Filter: c.name LIKE '%keyword%'

                    ===========================
                    PRICE RULE
                    ===========================
                    Effective price =
                      COALESCE(pv.variant_price, p.final_price, p.discount_price, p.price)

                    Price tolerance (±20%):
                        If user gives number N → range N*0.8 to N*1.2

                    Always LEFT JOIN pv when price is involved.

                    ===========================
                    BRAND RULE
                    ===========================
                    p.brand_name LIKE '%keyword%'

                    ===========================
                    ATTRIBUTE RULE (Dynamic + Fuzzy)
                    ===========================
                    JOIN pav + ca

                    Fuzzy match attribute name:
                        LOWER(ca.attribute_name) LIKE LOWER('%keyword%')
                     OR LOWER(ca.attribute_label) LIKE LOWER('%keyword%')

                    If numeric attribute → extract number:
                        CAST(REGEXP_REPLACE(pav.value, '[^0-9.]','') AS DECIMAL(18,2))

                    Numeric tolerance:
                        ±30%

                    IMPORTANT:
                    - If no attribute in schema matches the keyword → DO NOT apply any attribute filter.
                    - Never generate impossible conditions.

                    ===========================
                    COMBO LOGIC
                    ===========================
                    If user requests multi-component combo:
                        WHERE c.name IN ('loa','micro','mixer','amply','dac')

                    ===========================
                    FINAL FORMAT
                    ===========================
                    SELECT DISTINCT p.product_id
                    FROM ...
                    JOIN ...
                    WHERE ...
                    LIMIT 20
                    """),
            UserMessage.from(fullPrompt)
        );

        return response.content().text();
    }
}