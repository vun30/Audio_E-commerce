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

    //aAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    //aAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee; // phí ship GHN  dự kến cho đơn của từng store

    @Column(name = "shipping_fee_real")
    private BigDecimal shippingFeeReal; // phí ship GHN thực tế cho đơn của từng store

    @Column(name = "shipping_fee_for_store")
    private BigDecimal shippingFeeForStore; // phí ship chên lêch đơn thật GHN - Phí dự kiến khách trả

    //aAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    //aAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA


    @Column(name = "store_voucher_discount", precision = 18, scale = 2)
    private BigDecimal storeVoucherDiscount = BigDecimal.ZERO; // giảm do voucher shop của chính store này

    @Column(name = "platform_voucher_discount", precision = 18, scale = 2)
    private BigDecimal platformVoucherDiscount = BigDecimal.ZERO; // phần giảm platform phân bổ vào store này

    @Column(name = "shipping_service_type_id")
    private Integer shippingServiceTypeId;


    @Lob
    @Column(name = "store_voucher_detail_json")
    private String storeVoucherDetailJson; // {"CODE1":10000,"CODE2":15000}

    @Lob
    @Column(name = "platform_voucher_detail_json")
    private String platformVoucherDetailJson; // {"PLAT_CODE_1":20000}
    // =========================
    // 🏠 Shipping snapshot từ Customer
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
    @Column(
            name = "paid_by_shop",
            nullable = false,
            columnDefinition = "TINYINT(1) DEFAULT 0"
    )
    private Boolean paidByShop = false;


//    @Column(name = "paid_ship_price",nullable = true)
//    private double paidShipPriceByShop;

    // ====== Settlement breakdown cho shop ======

    @Column(name = "platform_fee_amount", precision = 18, scale = 2)
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;   // phí nền tảng (theo % product)

    // phí ship thực tế GHN báo về
    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage;   // % phí nền tảng tại thời điểm checkout (snapshot từ PlatformFee)

    @Column(name = "actual_shipping_fee", precision = 18, scale = 2)
    private BigDecimal actualShippingFee = BigDecimal.ZERO;   // GHN báo về (GhnOrder.totalFee)

    @Column(name = "shipping_extra_for_store", precision = 18, scale = 2)
    private BigDecimal shippingExtraForStore = BigDecimal.ZERO; // phần chênh GHN - shippingFee khách trả


    @Column(name = "net_payout_to_store", precision = 18, scale = 2)
    private BigDecimal netPayoutToStore = BigDecimal.ZERO;    // tiền cuối cùng chuyển vào ví shop

    @Lob
    @Column(name = "settlement_detail_json")
    private String settlementDetailJson; // JSON chi tiết breakdown


    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            totalAmount = BigDecimal.ZERO;
        } else {
            totalAmount = items.stream()
                    .map(StoreOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (discountTotal == null) discountTotal = BigDecimal.ZERO;
        if (storeVoucherDiscount == null) storeVoucherDiscount = BigDecimal.ZERO;
        if (platformVoucherDiscount == null) platformVoucherDiscount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;

        // đảm bảo discountTotal = store + platform
        discountTotal = storeVoucherDiscount.add(platformVoucherDiscount);

        grandTotal = totalAmount
                .subtract(discountTotal)
                .add(shippingFee);

        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;
    }
}
