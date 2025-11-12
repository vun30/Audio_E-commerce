package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AiQueryRequest;
import org.example.audio_ecommerce.dto.response.AiQueryResponse;

public interface AiQueryService {

    /**
     * ADMIN gọi để nạp schema toàn cục cho AI (chạy 1 lần duy nhất)
     */
    String initSchema();

    /**
     * Xử lý câu hỏi truy vấn dữ liệu (AI sinh SQL)
     */
    AiQueryResponse handleUserQuery(AiQueryRequest request);

    /**
     * Chat tự do với AI, phân biệt session theo userId
     */
    String chatWithGemini(AiQueryRequest request);
}
