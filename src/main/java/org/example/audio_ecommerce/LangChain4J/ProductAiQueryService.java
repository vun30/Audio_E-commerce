package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductAiQueryService {

    private final ProductQueryAgent agent;
    private final SqlExecutorTool sqlExecutor;
    private final SchemaLoader schemaLoader;
    private final ProductRepository productRepository;

    private final AiChatMemoryService memoryService;

    public Map<String, Object> searchProduct(String userId, String naturalQuestion) {

        // (1) Save user question
        memoryService.saveUserMessage(userId, naturalQuestion);
        memoryService.touchTtl(userId);

        // Load last messages (DISABLED CONTEXT)
        // List<ChatMessage> history = memoryService.loadMemory(userId);
        // StringBuilder historyText = new StringBuilder();
        // for (ChatMessage msg : history) {
        //     if (msg instanceof UserMessage) {
        //         historyText.append("User: ").append(msg).append("\n");
        //     } else if (msg instanceof AiMessage) {
        //         historyText.append("Assistant: ").append(msg).append("\n");
        //     }
        // }

        // Build prompt (WITHOUT CONTEXT)
        String fullPrompt = """
            You are an expert SQL generator for an Audio E-Commerce platform.

            ===========================
            USER REQUEST
            ===========================
            %s

            ===========================
            SCHEMA
            ===========================
            %s
            """.formatted(naturalQuestion, schemaLoader.loadSchema());

        // Generate SQL (DISABLED)
        // String sql = agent.generateSql(fullPrompt);

        // List<Map<String, Object>> rows;
        // try {
        //     rows = sqlExecutor.runSelect(sql);
        // } catch (Exception e) {
        //     return Map.of(
        //         "count", 0,
        //         "error", e.getMessage(),
        //         "sql", sql,
        //         "message", "AI generate SQL error"
        //     );
        // }

        // Convert to product IDs (DISABLED)
        List<Map<String, Object>> rows = new ArrayList<>();

        // Convert to product IDs
        List<UUID> productIds = rows.stream()
                .map(r -> UUID.fromString(r.get("product_id").toString()))
                .distinct()
                .toList();

        if (productIds.isEmpty()) {
            return Map.of(
                "count", 0,
                "message", "Không tìm thấy sản phẩm phù hợp.",
                "warnings", List.of("Không có sản phẩm phù hợp với yêu cầu hiện tại."),
                "items", List.of()
            );
        }

        // Load products
        List<Product> products = productRepository.findAllById(productIds);

        // Group categories for combo logic
        Map<String, List<Product>> grouped = groupProductsByCategory(products);

        boolean isCombo = naturalQuestion.toLowerCase().contains("combo")
                       || naturalQuestion.toLowerCase().contains("kara")
                       || naturalQuestion.toLowerCase().contains("karaoke");

        List<String> warnings = new ArrayList<>();
        List<String> requiredCates = List.of("loa", "micro", "mic", "dac", "mixer");

        if (isCombo) {
            for (String cate : requiredCates) {
                boolean exists = grouped.keySet().stream()
                        .anyMatch(k -> k.toLowerCase().contains(cate));
                if (!exists) warnings.add("Thiếu thành phần combo karaoke: " + cate);
            }
        }

        // Build summaries
        List<Map<String, Object>> summaries = products.stream()
                .map(this::buildSummary)
                .toList();

        // IMPORTANT: Không lưu search summary vào memory nữa để tránh "tìm theo ngữ cảnh".
        // Nếu muốn tìm lại, user phải search lại hoặc gửi productId.
        memoryService.touchTtl(userId);

        return Map.of(
            "count", productIds.size(),
            "productIds", productIds,
            "warnings", warnings,
            "message", warnings.isEmpty()
                    ? "Tìm thấy " + productIds.size() + " sản phẩm phù hợp"
                    : "Tìm thấy sản phẩm nhưng combo chưa đầy đủ",
            "items", summaries
        );
    }

    private Map<String, Object> buildSummary(Product p) {

        BigDecimal effectivePrice =
            p.getFinalPrice() != null ? p.getFinalPrice() :
            p.getDiscountPrice() != null ? p.getDiscountPrice() :
            p.getPrice();

        String priceStr = effectivePrice != null
                ? "%,d VND".formatted(effectivePrice.longValue())
                : "Không rõ giá";

        String rating = p.getRatingAverage() != null
                ? p.getRatingAverage() + "/5"
                : "Chưa có đánh giá";

        return Map.of(
                "productId", p.getProductId(),
                "name", p.getName(),
                "brand", p.getBrandName(),
                "rating", rating,
                "effectivePrice", priceStr,
                "summary", p.getBrandName() + " " + p.getName() + " – giá " + priceStr + ", đánh giá " + rating,
                "description", p.getDescription() // Thêm mô tả sản phẩm
        );
    }

    private Map<String, List<Product>> groupProductsByCategory(List<Product> products) {

        Map<String, List<Product>> grouped = new HashMap<>();

        for (Product p : products) {
            if (p.getCategories() == null) continue;

            for (var c : p.getCategories()) {
                if (c == null || c.getName() == null) continue;

                String key = c.getName().toLowerCase();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }
        return grouped;
    }

    /**
     * Tìm kiếm thông tin sản phẩm dựa trên productId và lưu làm LAST_PRODUCT
     */
    public Optional<Product> findAndAdviseProduct(String userId, String productId) {
        try {
            UUID productUUID = UUID.fromString(productId);
            Optional<Product> productOpt = productRepository.findById(productUUID);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                StringBuilder productInfo = new StringBuilder();
                productInfo.append("productId: ").append(product.getProductId()).append("\n")
                        .append("name: ").append(nullToEmpty(product.getName())).append("\n")
                        .append("brand: ").append(nullToEmpty(product.getBrandName())).append("\n")
                        .append("price: ").append(product.getFinalPrice() != null ? product.getFinalPrice() : product.getPrice()).append("\n")
                        .append("description: ").append(nullToEmpty(product.getDescription()));

                if (product.getAttributeValues() != null && !product.getAttributeValues().isEmpty()) {
                    productInfo.append("\nattributes:\n");
                    for (var attributeValue : product.getAttributeValues()) {
                        if (attributeValue.getAttribute() != null) {
                            productInfo.append("- ")
                                    .append(attributeValue.getAttribute().getAttributeName())
                                    .append(": ")
                                    .append(attributeValue.getValue())
                                    .append("\n");
                        }
                    }
                }

                memoryService.saveLastProductInfo(userId, productInfo.toString().trim());

                return Optional.of(product);
            }
        } catch (IllegalArgumentException e) {
            memoryService.saveLastProductInfo(userId, "productId không hợp lệ: " + productId);
        }

        return Optional.empty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
