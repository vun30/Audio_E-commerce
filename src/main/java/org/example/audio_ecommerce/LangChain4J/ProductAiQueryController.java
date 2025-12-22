package org.example.audio_ecommerce.LangChain4J;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai/products")
@RequiredArgsConstructor
public class ProductAiQueryController {

    private final ProductAiQueryService aiService;
    private final ProductQueryIntentDetector intentDetector;
    private final AudioChatService audioChatService;

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody AiQueryRequest request) {

        String question = request.getQuestion();
        String userId = (request.getUserId() == null || request.getUserId().isBlank())
                ? "ANONYMOUS_USER"
                : request.getUserId();

        // Kiểm tra xem câu hỏi có chứa ID sản phẩm không
        String productId = extractProductId(question);
        if (productId != null) {
            Optional<Product> productOpt = aiService.findAndAdviseProduct(userId, productId);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                Map<String, Object> productData = Map.of(
                    "productId", product.getProductId(),
                    "name", product.getName(),
                    "brandName", product.getBrandName(),
                    "categories", product.getCategories().stream().map(category -> Map.of(
                        "categoryId", category.getCategoryId(),
                        "categoryName", category.getName()
                    )).toList(),
                    "attributes", product.getAttributeValues().stream().map(attr -> Map.of(
                        "attributeId", attr.getAttribute().getAttributeId(),
                        "attributeName", attr.getAttribute().getAttributeName(),
                        "attributeLabel", attr.getAttribute().getAttributeLabel(),
                        "dataType", attr.getAttribute().getDataType(),
                        "value", attr.getValue()
                    )).toList()
                );


                return ResponseEntity.ok(Map.of(
                        "message", "Thông tin sản phẩm đã được lưu vào bộ nhớ AI.",
                        "product", productData
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Không tìm thấy sản phẩm với ID: " + productId
                ));
            }
        }

        // Nếu không có ID sản phẩm, tiếp tục xử lý câu hỏi bình thường
        String intent = intentDetector.detectIntent(question);

        switch (intent) {
            case "ADVICE" -> {
                String reply = audioChatService.chat(userId, question);
                return ResponseEntity.ok(Map.of(
                        "mode", "advice",
                        "question", question,
                        "reply", reply
                ));
            }

            case "SEARCH" -> {
                Map<String, Object> result = aiService.searchProduct(userId, question);
                return ResponseEntity.ok(Map.of(
                        "mode", "product_search",
                        "question", question,
                        "result", result
                ));
            }

            default -> {
                String reply = audioChatService.chat(userId, question);
                return ResponseEntity.ok(Map.of(
                        "mode", "none",
                        "question", question,
                        "reply", reply
                ));
            }
        }
    }

    private String extractProductId(String question) {
        // Logic để phát hiện ID sản phẩm trong câu hỏi
        // Giả sử ID sản phẩm là UUID
        String uuidRegex = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(uuidRegex);
        java.util.regex.Matcher matcher = pattern.matcher(question);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    @PostMapping("/api/products/advise")
    public ResponseEntity<?> adviseProduct(@RequestParam String userId, @RequestParam String productId) {
        Optional<Product> productOpt = aiService.findAndAdviseProduct(userId, productId);

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Map<String, Object> productData = Map.of(
                "productId", product.getProductId(),
                "name", product.getName(),
                "brandName", product.getBrandName(),
                "categories", product.getCategories().stream().map(category -> Map.of(
                    "categoryId", category.getCategoryId(),
                    "categoryName", category.getName()
                )).toList(),
                "attributes", product.getAttributeValues().stream().map(attr -> Map.of(
                    "attributeId", attr.getAttribute().getAttributeId(),
                    "attributeName", attr.getAttribute().getAttributeName(),
                    "attributeLabel", attr.getAttribute().getAttributeLabel(),
                    "dataType", attr.getAttribute().getDataType(),
                    "value", attr.getValue()
                )).toList()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Thông tin sản phẩm đã được lưu vào bộ nhớ AI.",
                    "product", productData
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "Không tìm thấy sản phẩm với ID: " + productId
            ));
        }
    }

    /**
     * Compatibility route: some clients accidentally call /api/ai/products/api/products/advise (double prefix).
     * Keep this to avoid 404 and forward to the correct advise handler.
     */
    @PostMapping("/api/products/advise")
    public ResponseEntity<?> adviseProductCompat(@RequestParam String userId, @RequestParam String productId) {
        return adviseProduct(userId, productId);
    }
}