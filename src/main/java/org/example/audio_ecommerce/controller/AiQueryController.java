package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AiQueryRequest;
import org.example.audio_ecommerce.dto.response.AiQueryResponse;
import org.example.audio_ecommerce.service.AiQueryService;
import org.example.audio_ecommerce.util.GeminiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "🤖 AI Assistant", description = "Giao tiếp và truy vấn thông minh với Gemini AI")
public class AiQueryController {

    private final AiQueryService aiQueryService;
    private final GeminiClient geminiClient;

    // ============================================================
    // ⚙️ ADMIN NẠP SCHEMA TOÀN CỤC
    // ============================================================
    @Operation(summary = "⚙️ ADMIN: Nạp cấu trúc bảng Product vào Gemini (toàn cục, gọi 1 lần duy nhất)")
    @PostMapping("/init-schema")
    public ResponseEntity<Map<String, String>> initSchema() {
        String result = aiQueryService.initSchema();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", result
        ));
    }

    // ============================================================
    // 💬 CHAT TỰ DO (KHÔNG CẦN SCHEMA)
    // ============================================================
    @Operation(summary = "💬 Chat tự do với Gemini (phân biệt userId để giữ hội thoại riêng)")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody AiQueryRequest request) {
        String answer = aiQueryService.chatWithGemini(request);
        return ResponseEntity.ok(Map.of(
                "userId", request.getUserId(),
                "userName", request.getUserName(),
                "message", request.getMessage(),
                "answer", answer
        ));
    }

    // ============================================================
    // 🧠 SINH SQL QUERY (CẦN SCHEMA)
    // ============================================================
    @Operation(summary = "🧠 Sinh SQL query từ câu hỏi người dùng (giới hạn category hợp lệ, phân biệt user)")
    @PostMapping("/query")
    public ResponseEntity<AiQueryResponse> query(@RequestBody AiQueryRequest request) {
        AiQueryResponse response = aiQueryService.handleUserQuery(request);
        return ResponseEntity.ok(response);
    }

     @PostMapping("/clear-memory")
    public String clearMemory() {
        geminiClient.clearAllData();
        return "🧽 Đã xoá toàn bộ dữ liệu schema và hội thoại khỏi bộ nhớ GeminiClient.";
    }
}
