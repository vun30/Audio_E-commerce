package org.example.audio_ecommerce.LangChain4J;

import org.springframework.stereotype.Component;

@Component
public class SchemaLoader {

    public String loadSchema() {
        return """
                ============================
                TABLE: products
                Columns:
                  product_id,
                  name,
                  brand_name,
                  price,
                  discount_price,
                  final_price

                ============================
                TABLE: product_variants
                Columns:
                  id,
                  product_id,
                  option_name,
                  option_value,
                  variant_price

                Variant Logic:
                  - Một product có thể có nhiều biến thể (màu, size, version...)
                  - Nếu có variant_price → dùng variant_price làm giá hiệu lực cho biến thể đó
                  - Nếu có nhiều biến thể → giá hiệu lực MIN = biến thể rẻ nhất
                  - Nếu user không chỉ rõ biến thể → luôn ưu tiên giá thấp nhất

                ============================
                TABLE: product_categories
                Columns:
                  product_id,
                  category_id

                ============================
                TABLE: categories
                Columns:
                  category_id,
                  name,
                  parent_id

                ============================
                TABLE: category_attributes
                Columns:
                  attribute_id,
                  category_id,
                  attribute_name,
                  attribute_label,
                  data_type

                Notes:
                  attribute_name & attribute_label dùng cho fuzzy match.
                  Example fuzzy: "delay" ~ "latency" ~ "response_time"
                  Nếu không match attribute → bỏ qua filter attribute.

                ============================
                TABLE: product_attribute_values
                Columns:
                  id,
                  product_id,
                  category_attribute_id,
                  value

                ============================
                PRICE LOGIC (Updated with Variant Logic)
                Effective price =
                    COALESCE(
                        pv.variant_price,        -- ưu tiên biến thể nếu có
                        p.final_price,
                        p.discount_price,
                        p.price
                    )

                If a product has multiple variants:
                    effective_price = MIN(pv.variant_price)

                Budget tolerance: ±20%

                ============================
                ATTRIBUTE LOGIC
                Numeric extraction:
                    CAST(REGEXP_REPLACE(value, '[^0-9.]','') AS DECIMAL)
                Numeric tolerance: ±30%

                Only apply attribute filtering if fuzzy match succeeds.

                ============================
                VARIANT SEARCH LOGIC
                - Nếu user hỏi "màu", "phiên bản", "bản bluetooth", "bản USB":
                    → JOIN pv + filter option_value fuzzy match
                - Nếu user không nói biến thể:
                    → vẫn JOIN pv để lấy giá chính xác nhất (biến thể rẻ nhất)
                - Ưu tiên:
                    variant_price > final_price > discount_price > price

                ============================
                """;
    }
}
