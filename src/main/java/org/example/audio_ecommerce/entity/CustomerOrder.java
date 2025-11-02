package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_order")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "message", length = 512)
    private String message; // tin nhắn từ khách hàng (nếu có)

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderItem> items = new ArrayList<>();

    @Column(name = "discount_total", precision = 18, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // entity/CustomerOrder.java
    @Column(name = "shipping_fee_total")
    private BigDecimal shippingFeeTotal; // tổng phí ship tất cả store


    @Column(name = "external_order_code")
    private String externalOrderCode;

    // trong CustomerOrder.java (chỉ phần bổ sung)
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

    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        // Tính items subtotal (totalAmount) dựa vào CustomerOrderItem
        if (items == null || items.isEmpty()) {
            totalAmount = BigDecimal.ZERO;
        } else {
            totalAmount = items.stream()
                    .map(CustomerOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (discountTotal == null)   discountTotal   = BigDecimal.ZERO;
        if (shippingFeeTotal == null) shippingFeeTotal = BigDecimal.ZERO;

        // grandTotal = itemsSubtotal - discountTotal + shippingFeeTotal
        grandTotal = totalAmount
                .subtract(discountTotal)
                .add(shippingFeeTotal);

        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }
    }

}