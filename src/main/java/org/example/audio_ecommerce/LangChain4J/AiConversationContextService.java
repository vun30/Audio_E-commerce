//package org.example.audio_ecommerce.LangChain4J;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class AiConversationContextService {
//
//    private final AiConversationContextRepository repo;
//
//    public void setProductContext(
//            String userId,
//            String productId,
//            String productName
//    ) {
//        AiConversationContext ctx =
//                repo.findById(userId).orElse(new AiConversationContext());
//
//        ctx.setUserId(userId);
//        ctx.setMode("PRODUCT_ADVICE");
//        ctx.setLastProductId(productId);
//        ctx.setLastProductName(productName);
//        ctx.setUpdatedAt(LocalDateTime.now());
//
//        repo.save(ctx);
//    }
//
//    public Optional<AiConversationContext> getContext(String userId) {
//        return repo.findById(userId);
//    }
//
//    public void clearContext(String userId) {
//        repo.deleteById(userId);
//    }
//}
