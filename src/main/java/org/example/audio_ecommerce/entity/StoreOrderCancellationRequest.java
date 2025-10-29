// entity/StoreOrderCancellationRequest.java
package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CancellationReason;
import org.example.audio_ecommerce.entity.Enum.CancellationRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "store_order_cancellation")
public class StoreOrderCancellationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancellationReason reason;

    @Column(length = 512)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancellationRequestStatus status = CancellationRequestStatus.REQUESTED;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;
}
