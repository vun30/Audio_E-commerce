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
                  attribute_name and attribute_label are used for fuzzy matching.
                  Example fuzzy match: keyword "delay" can match "latency", "response_time"
                  Only apply attribute filtering if fuzzy match finds a valid attribute.

                ============================
                TABLE: product_attribute_values
                Columns:
                  id,
                  product_id,
                  category_attribute_id,
                  value

                ============================
                PRICE LOGIC
                Effective price =
                    COALESCE(pv.variant_price, p.final_price, p.discount_price, p.price)
                Tolerance: ±20%

                ATTRIBUTE LOGIC
                Numeric extraction:
                    CAST(REGEXP_REPLACE(value, '[^0-9.]','') AS DECIMAL)
                Tolerance: ±30%

                Fuzzy rules:
                    If no attribute matches keyword, skip attribute filter entirely.
                ============================
                """;
    }
}


