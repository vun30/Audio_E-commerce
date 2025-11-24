package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.entity.Enum.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_target_targetId", columnList = "target,targetId"),
        @Index(name = "idx_notif_is_read", columnList = "is_read")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationTarget target;          // STORE, CUSTOMER, ADMIN...

    @Column(nullable = false)
    private UUID targetId;                      // storeId hoặc customerId...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;              // NEW_ORDER, ORDER_PAID,...

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    private String actionUrl;                   // ví dụ: /seller/orders/{id}

    @Column(columnDefinition = "TEXT")
    private String metadataJson;                // optional: JSON detail
}
