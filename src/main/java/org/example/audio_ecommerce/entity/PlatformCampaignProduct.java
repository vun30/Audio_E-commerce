package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "platform_campaign_products")
public class PlatformCampaignProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔗 Quan hệ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PlatformCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 🔗 Slot tham gia (Fast Sale)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_slot_id")
    private PlatformCampaignFlashSlot flashSlot;

    // =======================================================
    // 💰 THÔNG TIN GIẢM GIÁ (Voucher logic)
    // =======================================================

    @Column(precision = 12, scale = 2)
    private BigDecimal discountValue; // Nếu FIXED: giảm tiền

    @Column
    private Integer discountPercent; // Nếu PERCENT: giảm %

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountValue; // Giới hạn giảm tối đa

    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderValue; // Đơn hàng tối thiểu để áp dụng

    // =======================================================
    // 🔢 HẠN MỨC PHÁT HÀNH & SỬ DỤNG
    // =======================================================
    private Integer totalVoucherIssued;   // Số lượng phát hành
    private Integer totalUsageLimit;      // Tổng lượt dùng toàn hệ thống
    private Integer usagePerUser;         // Mỗi user dùng tối đa
    private Integer remainingUsage;       // Số lượt còn lại

    // org.example.audio_ecommerce.entity.PlatformCampaignProduct
// (Giữ các field bạn đã có, bổ sung bên dưới nếu thiếu)
@Enumerated(EnumType.STRING)
private VoucherType type; // FIXED / PERCENT / SHIPPING

@Column(precision = 12, scale = 2)
private BigDecimal originalPrice;

@Column(precision = 12, scale = 2)
private BigDecimal discountedPrice;

    // =======================================================
    // ⏰ THỜI GIAN & TRẠNG THÁI
    // =======================================================
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status; // DRAFT / ACTIVE / EXPIRED / DISABLED

    private Boolean approved = false;
    private LocalDateTime approvedAt;
    private LocalDateTime registeredAt;

    // =======================================================
    // 🧾 Metadata tracking
    // =======================================================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUpdatedAt;
    private Long lastUpdateIntervalDays;

    // =======================================================
    // ⚙️ LIFE-CYCLE HOOKS
    // =======================================================
    @PrePersist
public void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.remainingUsage = this.totalUsageLimit;
    if (this.status == null) { // ✅ chỉ set nếu chưa có
        this.status = VoucherStatus.DRAFT;
    }
}
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // =======================================================
    // 🚦 LOGIC CHÍNH THEO LUỒNG SHOPEE
    // =======================================================

    /**
     * 👑 B1: Admin tạo chiến dịch FAST_SALE
     * -> Admin tạo PlatformCampaign (FAST_SALE) + slots
     * -> Các store có thể đăng ký sản phẩm khi allowRegistration = true
     */

    /**
     * 👑 B2: Admin tạo các khung giờ (slots)
     * -> Thông tin lưu ở PlatformCampaignFlashSlot
     */

    /**
     * 👑 B3: Admin bật đăng ký (allowRegistration = true)
     * -> Cho phép các store gọi API đăng ký
     */

    /**
     * 🏪 B4: Store xem danh sách slot khả dụng
     * -> Store gọi API /api/campaigns/{id}/slots (status = PENDING)
     */

    /**
     * 🏪 B5: Store chọn slot & đăng ký sản phẩm
     * -> Lúc này onCreate() được gọi
     * -> status = DRAFT, approved = false
     */

    /**
     * 👑 B6: Admin duyệt sản phẩm (approved = true)
     * -> Gọi approveProduct() để set status = ACTIVE khi đến giờ slot
     */
    public void approveProduct() {
        this.approved = true;
        this.approvedAt = LocalDateTime.now();
        this.status = VoucherStatus.ACTIVE;
    }

    /**
     * ⏰ B7: Scheduler khi đến giờ mở slot → tự bật Flash Sale
     * -> Gọi activateIfInSlot()
     */
    public void activateIfInSlot(LocalDateTime now) {
        if (approved && now.isAfter(startTime) && now.isBefore(endTime)) {
            this.status = VoucherStatus.ACTIVE;
        }
    }

    /**
     * ⏰ B8: Scheduler khi hết giờ → tự đóng slot & ẩn sản phẩm
     * -> Gọi expireIfPassed()
     */
    public void expireIfPassed(LocalDateTime now) {
        if (now.isAfter(endTime)) {
            this.status = VoucherStatus.EXPIRED;
        }
    }
}
