package org.example.audio_ecommerce.LangChain4J;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
