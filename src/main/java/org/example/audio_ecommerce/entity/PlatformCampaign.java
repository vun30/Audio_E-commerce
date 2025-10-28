package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "platform_campaigns")
public class PlatformCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔹 Mã chương trình duy nhất
    @Column(nullable = false, unique = true, length = 50)
    private String code; // "FAST_SALE", "MEGA_SALE"

    // 🔹 Tên chương trình hiển thị
    @Column(nullable = false, length = 100)
    private String name; // "Flash Sale 11.11", "Mega Sale 12.12"

    @Column(length = 1000)
    private String description;

    // =========================
    // ⚙️ PHÂN LOẠI CHƯƠNG TRÌNH
    // =========================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignType campaignType; // SHOP_LEVEL / PRODUCT_LEVEL

    // 🎨 Thông tin hiển thị badge
    private String badgeLabel;    // "Flash Sale", "Mega Sale"
    private String badgeColor;    // "#FF6600"
    private String badgeIconUrl;  // "https://cdn.audiohub.vn/badges/flashsale.png"

    // 📅 Thời gian tổng thể (Mega Sale dùng, Fast Sale vẫn cần để xác định khoảng ngày)
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status; // ACTIVE / CLOSED / DRAFT

    private Boolean allowRegistration = true;

    @Column(length = 500)
    private String approvalRule; // "Trước 17h kích hoạt 0h hôm sau..."

    private LocalDateTime createdAt;
    private UUID createdBy; // adminId

    // =========================
    // 🔗 QUAN HỆ
    // =========================
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlatformCampaignStore> participatingStores = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlatformCampaignProduct> participatingProducts = new ArrayList<>();

    // 🕒 Flash Sale khung giờ (chỉ dùng cho FAST_SALE)
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlatformCampaignFlashSlot> flashSlots = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (status == null) this.status = VoucherStatus.ACTIVE;
    }
}
