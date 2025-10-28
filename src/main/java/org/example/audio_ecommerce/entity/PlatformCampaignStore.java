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
@Table(name = "platform_campaign_stores")
public class PlatformCampaignStore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔗 Quan hệ với campaign
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PlatformCampaign campaign;

    // 🔗 Quan hệ với store
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // ⚙️ Trạng thái đăng ký
    private Boolean approved = false;
    private LocalDateTime registeredAt;
    private LocalDateTime approvedAt;

    @PrePersist
    public void onRegister() {
        this.registeredAt = LocalDateTime.now();
        this.approved = false;
    }
}
