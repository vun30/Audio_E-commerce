package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.SlotStatus;  // ✅ import enum riêng

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "platform_campaign_flash_slots")
public class PlatformCampaignFlashSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔗 Liên kết với campaign
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PlatformCampaign campaign;

    // 🕒 Khung giờ
    @Column(nullable = false)
    private LocalDateTime openTime;

    @Column(nullable = false)
    private LocalDateTime closeTime;

    // ⚙️ Trạng thái (PENDING / ACTIVE / CLOSED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) status = SlotStatus.PENDING;
    }
}
