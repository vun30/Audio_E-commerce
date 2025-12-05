package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payout_bill")
public class PayoutBill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shop_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID shopId;

    @Column(name = "bill_code", length = 20, nullable = false)
    private String billCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "from_date", nullable = false)
    private LocalDateTime fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDateTime toDate;

    // ===== Tổng theo bill =====
    @Column(name = "total_gross", precision = 18, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_platform_fee", precision = 18, scale = 2)
    private BigDecimal totalPlatformFee = BigDecimal.ZERO;

    @Column(name = "total_shipping_order_fee", precision = 18, scale = 2)
    private BigDecimal totalShippingOrderFee = BigDecimal.ZERO;

    @Column(name = "total_return_shipping_fee", precision = 18, scale = 2)
    private BigDecimal totalReturnShippingFee = BigDecimal.ZERO;

    @Column(name = "total_net_payout", precision = 18, scale = 2)
    private BigDecimal totalNetPayout = BigDecimal.ZERO;

    // ====== Trạng thái bill payout ======
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private PayoutBillStatus status = PayoutBillStatus.PENDING;

    // ====== Mã chuyển khoản / payment reference ======
    @Column(name = "transfer_reference", length = 128)
    private String transferReference;

    // ====== Ảnh chứng từ chuyển khoản ======
    @Column(name = "receipt_image_url", length = 512)
    private String receiptImageUrl;

    // ====== Ghi chú từ admin ======
    @Column(name = "admin_note", length = 1024)
    private String adminNote;

    // ===== Các bảng con =====
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayoutBillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayoutShippingOrderFee> shippingOrders = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayoutReturnShippingFee> returnShipFees = new ArrayList<>();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
