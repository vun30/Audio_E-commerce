package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payout_bill_item")
public class PayoutBillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private PayoutBill bill;

    @Column(name = "order_item_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID orderItemId;

    @Column(name = "store_order_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID storeOrderId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "is_returned")
    private Boolean isReturned = false;

    @Column(name = "final_line_total", precision = 18, scale = 2)
    private BigDecimal finalLineTotal; // số tiền sau giảm

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage;

    @Column(name = "platform_fee_amount", precision = 18, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "net_payout", precision = 18, scale = 2)
    private BigDecimal netPayout;

    @PrePersist
    public void preCalc() {
        if (platformFeePercentage == null) platformFeePercentage = BigDecimal.ZERO;
        if (finalLineTotal == null) finalLineTotal = BigDecimal.ZERO;

        // Tính platform fee
        this.platformFeeAmount = finalLineTotal
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100));

        // Net payout item
        this.netPayout = finalLineTotal.subtract(platformFeeAmount);
    }
}
