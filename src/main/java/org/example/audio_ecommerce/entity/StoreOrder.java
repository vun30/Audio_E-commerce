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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee; // phí ship GHN cho đơn của từng store

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
