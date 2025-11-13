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

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String question) {

        var result = aiService.searchProduct(question);

        return ResponseEntity.ok(
                Map.of(
                        "question", question,
                        "resultProductIds", result
                )
        );
    }
}
