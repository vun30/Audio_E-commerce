package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "shop_voucher_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shop_voucher_customer",
                        columnNames = {"voucher_id", "customer_id"}
                )
        }
)
public class ShopVoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private ShopVoucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer usedCount;   // đã dùng bao nhiêu lần

    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;

    @PrePersist
    public void onCreate() {
        if (usedCount == null) usedCount = 0;
    }
}
