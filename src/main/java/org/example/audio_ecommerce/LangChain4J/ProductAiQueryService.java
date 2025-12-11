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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAiQueryService {

    private final ProductQueryAgent agent;
    private final SqlExecutorTool sqlExecutor;
    private final SchemaLoader schemaLoader;
    private final ProductRepository productRepository;

    private final AiChatMemoryService memoryService;   // 👈 THÊM MEMORY

    /**
     * Query Agent có nhớ context chat của user
     */
   public Map<String, Object> searchProduct(String userId, String naturalQuestion) {

    // (1) SAVE USER MESSAGE
    memoryService.saveUserMessage(userId, naturalQuestion);

    // 0) LOAD HISTORY
    List<ChatMessage> history = memoryService.loadMemory(userId);
    StringBuilder historyText = new StringBuilder();

    for (ChatMessage msg : history) {
        if (msg instanceof UserMessage) {
            historyText.append("User: ")
                       .append(((UserMessage) msg).text())
                       .append("\n");
        } else if (msg instanceof AiMessage) {
            historyText.append("Assistant: ")
                       .append(((AiMessage) msg).text())
                       .append("\n");
        }
    }

    // 1) Build full prompt with HISTORY
    String fullPrompt = """
            You are an expert SQL generator for an Audio E-Commerce platform.

            ===========================
            CONVERSATION HISTORY (Context)
            ===========================
            %s

            ===========================
            CURRENT USER REQUEST
            ===========================
            %s

            ===========================
            DATABASE SCHEMA
            ===========================
            %s
            """.formatted(historyText.toString(), naturalQuestion, schemaLoader.loadSchema());

    // 2) Generate SQL
    String sql = agent.generateSql(fullPrompt);

    List<Map<String, Object>> rows;
    try {
        rows = sqlExecutor.runSelect(sql);
    } catch (Exception e) {
        return Map.of(
            "count", 0,
            "error", e.getMessage(),
            "sql", sql,
            "message", "AI generate SQL error"
        );
    }

    // 3) Convert product IDs
    List<UUID> productIds = rows.stream()
            .map(r -> UUID.fromString(r.get("product_id").toString()))
            .distinct()
            .toList();

    if (productIds.isEmpty()) {
        return Map.of(
                "count", 0,
                "message", "Không tìm thấy sản phẩm phù hợp.",
                "warnings", List.of("Không có sản phẩm đúng theo lịch sử và yêu cầu hiện tại."),
                "items", List.of()
        );
    }

    List<Product> products = productRepository.findAllById(productIds);

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

    List<Map<String, Object>> summaries = products.stream()
            .map(this::buildSummary)
            .collect(Collectors.toList());

    // (2) SAVE AI SUMMARY MESSAGE
    memoryService.saveAssistantMessage(userId, "Found " + productIds.size() + " matching products.");

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
    // ======================================
    // SUMMARY BUILDER
    // ======================================
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

        String brand = p.getBrandName() != null ? p.getBrandName() : "Không rõ thương hiệu";
        String name = p.getName() != null ? p.getName() : "(Sản phẩm không tên)";

        return Map.of(
                "productId", p.getProductId(),
                "name", name,
                "brand", brand,
                "rating", rating,
                "effectivePrice", priceStr,
                "summary", brand + " " + name + " – giá ~" + priceStr + ", đánh giá " + rating
        );
    }

    // ======================================
    // GROUP BY CATEGORY
    // ======================================
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
}
