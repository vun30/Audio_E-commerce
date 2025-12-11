package org.example.audio_ecommerce.LangChain4J;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, Long> {

    List<AiChatHistory> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}