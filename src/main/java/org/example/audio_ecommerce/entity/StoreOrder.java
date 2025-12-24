package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_order")
public class StoreOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_code", length = 20)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 64, nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "storeOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<StoreOrderItem> items = new ArrayList<>();

    @Column(name = "discount_total", precision = 18, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    @JsonBackReference
    private CustomerOrder customerOrder;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // =========================
    // Shipping
    // =========================

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee; // phí ship dự kiến (khách trả / ước tính)

    @Column(name = "shipping_fee_real")
    private BigDecimal shippingFeeReal; // phí ship GHN thực tế

    @Column(name = "shipping_fee_for_store")
    private BigDecimal shippingFeeForStore; // chênh GHN thực tế - phí dự kiến

    @Builder.Default
    @Column(name = "cod_collected", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean codCollected = false;  // khác COD đã thu hộ chưa

//     // ✅ Số tiền COD đã thu hộ từ khách hàng
//    @Column(name = "cod_collected_amount", precision = 18, scale = 2)
//    private BigDecimal codCollectedAmount = BigDecimal.ZERO;

    // ✅ Phí ship bị tính cho shop khi đơn KHÔNG NHẬN / quay đầu (50% phí GHN thực tế)
    @Column(name = "return_shipping_charge", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal returnShippingCharge = BigDecimal.ZERO;

    @Column(name = "return_shipping_charge_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal returnShippingChargeRate = new BigDecimal("50.00"); // %

    // ✅ NEW: chống cộng lặp (idempotent) cho phí quay đầu
    @Column(name = "return_charge_applied", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean returnChargeApplied = false;

    // ✅ Tổng nợ đơn hàng (phí quay đầu + tiền hàng ship đi chưa thu được hoặc phí chênh lệch nếu cus nhận hàng va thanh toán)
    @Column(name = "total_debt_for_order", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDebtOrder = BigDecimal.ZERO;


    @Column(name = "return_charge_applied_at")
    private LocalDateTime returnChargeAppliedAt;

    // =========================
    // Voucher
    // =========================

    @Column(name = "store_voucher_discount", precision = 18, scale = 2)
    private BigDecimal storeVoucherDiscount = BigDecimal.ZERO;

    @Column(name = "platform_voucher_discount", precision = 18, scale = 2)
    private BigDecimal platformVoucherDiscount = BigDecimal.ZERO;

    @Column(name = "shipping_service_type_id")
    private Integer shippingServiceTypeId;

    @Lob
    @Column(name = "store_voucher_detail_json")
    private String storeVoucherDetailJson;

    @Lob
    @Column(name = "platform_voucher_detail_json")
    private String platformVoucherDetailJson;

    // =========================
    // Shipping snapshot từ Customer
    // =========================

    @Column(name = "ship_receiver_name", length = 255)
    private String shipReceiverName;

    @Column(name = "ship_phone_number", length = 30)
    private String shipPhoneNumber;

    @Column(name = "ship_country", length = 100)
    private String shipCountry;

    @Column(name = "ship_province", length = 120)
    private String shipProvince;

    @Column(name = "ship_district", length = 120)
    private String shipDistrict;

    @Column(name = "ship_ward", length = 120)
    private String shipWard;

    @Column(name = "ship_street", length = 255)
    private String shipStreet;

    @Column(name = "ship_address_line", length = 512)
    private String shipAddressLine;

    @Column(name = "ship_postal_code", length = 20)
    private String shipPostalCode;

    @Column(name = "ship_note", length = 512)
    private String shipNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 32, nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Builder.Default
    @Column(name = "paid_by_shop", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean paidByShop = false;

    @Builder.Default
    @Column(name = "ghn_debt_finalized", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean ghnDebtFinalized = false;

    // ====== Settlement breakdown cho shop ======

    @Column(name = "platform_fee_amount", precision = 18, scale = 2)
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage;

    @Column(name = "actual_shipping_fee", precision = 18, scale = 2)
    private BigDecimal actualShippingFee = BigDecimal.ZERO;

    @Column(name = "shipping_extra_for_store", precision = 18, scale = 2)
    private BigDecimal shippingExtraForStore = BigDecimal.ZERO;

    @Column(name = "net_payout_to_store", precision = 18, scale = 2)
    private BigDecimal netPayoutToStore = BigDecimal.ZERO;

    @Lob
    @Column(name = "settlement_detail_json",columnDefinition = "TEXT")
    private String settlementDetailJson; // JSON chi tiết breakdown

    @Builder.Default
    @Column(name = "store_scored", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean storeScored = false;

    @Column(nullable = false)
    private boolean sellCountUpdated = false;

    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            totalAmount = BigDecimal.ZERO;
        } else {
            totalAmount = items.stream()
                    .map(StoreOrderItem::getLinePriceBeforeDiscount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (discountTotal == null) discountTotal = BigDecimal.ZERO;
        if (storeVoucherDiscount == null) storeVoucherDiscount = BigDecimal.ZERO;
        if (platformVoucherDiscount == null) platformVoucherDiscount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;

        discountTotal = storeVoucherDiscount.add(platformVoucherDiscount);

        grandTotal = totalAmount
                .subtract(discountTotal);
//                .add(shippingFee);

        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;
    }
}
