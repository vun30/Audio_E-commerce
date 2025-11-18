package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "platform_revenue",
        indexes = {
                @Index(name = "idx_platform_revenue_day", columnList = "revenueDate"),
                @Index(name = "idx_platform_revenue_type", columnList = "type")
        })
public class PlatformRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "CHAR(36)")
    private UUID storeOrderId;     // Nếu có liên quan tới đơn cửa hàng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlatformRevenueType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate revenueDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (revenueDate == null) {
            revenueDate = LocalDate.now();
        }
        if (amount == null) amount = BigDecimal.ZERO;
    }
}
