package org.example.audio_ecommerce.LangChain4J;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductQueryIntentDetector {

    private final OpenAiChatModel chatModel;

    public String detectIntent(String userMessage) {

        var response = chatModel.generate(
                SystemMessage.from("""
                        You are an INTENT CLASSIFIER for an AUDIO ASSISTANT.
                        Always output ONLY ONE of the following labels:

                        SEARCH
                        ADVICE
                        NONE

                        ===========================
                        OUTPUT "SEARCH" IF:
                        ===========================
                        User clearly wants to FIND / BUY a product:
                        - tìm loa, tìm amply, tìm dac, tìm sub
                        - mua loa, mua amply, mua dac
                        - budget, giá bao nhiêu
                        - gợi ý sản phẩm
                        - combo karaoke, dàn xem phim, dàn nghe nhạc (khi yêu cầu có từ 'mua', 'tìm')
                        - "loa nào tốt", "amply nào hợp", "dac nào hợp"

                        ===========================
                        OUTPUT "ADVICE" IF:
                        ===========================
                        User is ASKING for AUDIO SETUP / MATCHING / EXPERT ADVICE:
                        - setup phòng nghe, phòng xem phim
                        - ghép loa + amply, matching
                        - chọn sub diện tích
                        - build dàn 2.1 / 5.1 / Atmos
                        - kỹ thuật âm học
                        - phối ghép DAC + ampli + loa
                        - tư vấn theo nhu cầu

                        ===========================
                        OUTPUT "NONE" IF:
                        ===========================
                        User asks unrelated topics:
                        - IT, crypto, coding, shipping, chính sách...
                        - hỏi linh tinh / xã giao

                        RULES:
                        - Must output EXACTLY one of: SEARCH / ADVICE / NONE
                        - No explanation.
                        """),
                UserMessage.from(userMessage)
        );

        return response.content().text().trim().toUpperCase();
    }
}
