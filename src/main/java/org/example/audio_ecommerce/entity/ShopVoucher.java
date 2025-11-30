package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shop_vouchers")
public class ShopVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔹 Quan hệ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Store shop;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopVoucherProduct> voucherProducts = new ArrayList<>();

    // 🔹 Thông tin cơ bản
    @Column(nullable = false, unique = true, length = 100)
    private String code; // VD: SALE10K

    @Column(nullable = false, length = 255)
    private String title; // Tiêu đề hiển thị

    @Column(length = 500)
    private String description;

    // 🔹 Cấu hình giảm giá
    @Enumerated(EnumType.STRING)
    private VoucherType type; // FIXED / PERCENT / SHIPPING

    @Column(precision = 12, scale = 2)
    private BigDecimal discountValue; // Nếu FIXED: giảm tiền

    @Column
    private Integer discountPercent; // Nếu PERCENT: giảm %

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountValue; // Giới hạn tối đa khi % giảm

    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderValue; // Đơn tối thiểu

    // 🔹 Hạn mức phát hành
    private Integer totalVoucherIssued;   // Số lượng phát hành
    private Integer usagePerUser;         // Mỗi user dùng tối đa
    private Integer remainingUsage;       // Số lượt còn lại

    // 🔹 Thời gian & trạng thái
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status; // DRAFT / ACTIVE / EXPIRED / DISABLED

    // 🔹 Metadata tracking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUpdatedAt;
    private Long lastUpdateIntervalDays;

    private UUID createdBy;
    private UUID updatedBy;

    @Enumerated(EnumType.STRING)
    private ShopVoucherScopeType scopeType; // PRODUCT_VOUCHER hoặc ALL_SHOP_VOUCHER

    // ===== Lifecycle Hooks =====
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;
        this.lastUpdateIntervalDays = 0L;
        // ✅ Nếu chưa set remainingUsage thì mặc định = totalVoucherIssued
        if (this.remainingUsage == null) {
            this.remainingUsage = this.totalVoucherIssued;
        }
    }

    @PreUpdate
    public void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.lastUpdatedAt == null) this.lastUpdatedAt = this.createdAt;
        this.lastUpdateIntervalDays = ChronoUnit.DAYS.between(this.lastUpdatedAt, now);
        this.lastUpdatedAt = this.updatedAt != null ? this.updatedAt : this.createdAt;
        this.updatedAt = now;
    }
}
