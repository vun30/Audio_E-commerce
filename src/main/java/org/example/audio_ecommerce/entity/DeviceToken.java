package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "device_tokens",
        indexes = {
                @Index(name = "idx_device_token_target", columnList = "target,targetId")
        }
)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTarget target; // CUSTOMER / STORE

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false, unique = true)
    private String token; // FCM token

    private String platform;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;
}
