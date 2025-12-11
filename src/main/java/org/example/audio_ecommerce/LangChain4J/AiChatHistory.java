package org.example.audio_ecommerce.LangChain4J;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistory {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String role;  // user / assistant

    @Column (columnDefinition = "LONGTEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}
