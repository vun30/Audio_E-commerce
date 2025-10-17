package org.example.audio_ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.hibernate.annotations.GenericGenerator;

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

    // ========== 🔹 Quan hệ ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Store shop; // Cửa hàng sở hữu voucher

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopVoucherProduct> voucherProducts = new ArrayList<>();

    // ========== 🔹 Thông tin cơ bản ==========
    @Column(nullable = false, unique = true, length = 100)
    private String code;  // Mã voucher (VD: SALE10K)

    @Column(nullable = false, length = 255)
    private String title; // Tiêu đề hiển thị

    @Column(length = 500)
    private String description; // Mô tả chi tiết

    // ========== 🔹 Cấu hình giảm giá ==========
    @Enumerated(EnumType.STRING)
    private VoucherType type; // FIXED / PERCENT / SHIPPING

    private BigDecimal discountValue; // Nếu FIXED: giảm tiền
    private Integer discountPercent;  // Nếu PERCENT: giảm %
    private BigDecimal maxDiscountValue; // Giảm tối đa khi theo %
    private BigDecimal minOrderValue; // Đơn tối thiểu

    // ========== 🔹 Phát hành & Hạn mức ==========
    private Integer totalVoucherIssued;   // Số lượng voucher phát hành
    private Integer totalUsageLimit;      // Tổng lượt dùng
    private Integer usagePerUser;         // Mỗi KH dùng tối đa
    private Integer remainingUsage;       // Số lượt còn lại

    // ========== 🔹 Thời gian & trạng thái ==========
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status; // DRAFT, ACTIVE, EXPIRED, DISABLED

     private LocalDateTime createdAt; // 📝 Ví dụ: 2025-01-15T10:30:00
    private LocalDateTime updatedAt; // 📝 Ví dụ: 2025-01-16T14:22:00
    private LocalDateTime lastUpdatedAt;    // thời điểm update gần nhất trước đó
    private Long lastUpdateIntervalDays;    // số ngày cách lần cập nhật trước
    private UUID createdBy; // 📝 Ví dụ: UUID("user-admin-123")
    private UUID updatedBy; // 📝 Ví dụ: UUID("user-seller-456")

    // ============= Gán thời điểm khi tạo mới =============
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdatedAt = now;          // xem như update gần nhất là khi tạo
        this.lastUpdateIntervalDays = 0L;  // lần đầu tạo => 0 ngày
    }

    // ============= Tự tính số ngày mỗi khi update =============
    @PreUpdate
    public void onUpdate() {
        LocalDateTime now = LocalDateTime.now();

        // nếu chưa có lastUpdatedAt thì dùng createdAt làm mốc
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = this.createdAt;
        }

        // tính số ngày giữa lần update trước và hiện tại
        this.lastUpdateIntervalDays =
                ChronoUnit.DAYS.between(this.lastUpdatedAt, now);

        // cập nhật lại mốc thời gian
        this.lastUpdatedAt = this.updatedAt != null ? this.updatedAt : this.createdAt;
        this.updatedAt = now;
    }
}
