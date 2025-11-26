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
        name = "platform_campaign_product_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_product_customer",
                        columnNames = {"campaign_product_id", "customer_id"}
                )
        }
)
public class PlatformCampaignProductUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_product_id", nullable = false)
    private PlatformCampaignProduct campaignProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer usedCount;

    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;

    @PrePersist
    public void onCreate() {
        if (usedCount == null) usedCount = 0;
    }
}
