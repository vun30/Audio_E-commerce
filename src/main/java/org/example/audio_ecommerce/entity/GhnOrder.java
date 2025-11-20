package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ghn_order",
        indexes = {
                @Index(name="idx_ghn_order_store_order", columnList = "store_order_id"),
                @Index(name="idx_ghn_order_store", columnList = "store_id"),
                @Index(name="idx_ghn_order_code", columnList = "order_ghn")
        })
public class GhnOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ghép cứng để dễ query; không dùng FK cứng để tránh vòng lặp nếu bạn muốn
    @Column(name="store_order_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID storeOrderId;

    @Column(name="store_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID storeId;

    @Column(name="order_ghn", length = 64, nullable = false)
    private String orderGhn;                // GHN order code

    @Column(name="total_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalFee;

    @Column(name="expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(name="status", length = 64, nullable = false)
    private GhnStatus status;
    // ví dụ: "ready_to_pick", "picking", "delivering", "delivered", ...

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
