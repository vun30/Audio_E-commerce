package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "store_revenue",
        indexes = {
                @Index(name = "idx_store_revenue_store", columnList = "storeId"),
                @Index(name = "idx_store_revenue_day", columnList = "revenueDate")
        })
public class StoreRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Không dùng FK cứng để tránh phức tạp, chỉ lưu ID cho dễ query
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private UUID storeId;

    @Column(columnDefinition = "CHAR(36)")
    private UUID storeOrderId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;         // Tiền shop thực nhận

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal feePlatform;    // Phí nền tảng thu

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal feeShipping;    // Chênh lệch ship GHN shop phải trả (+) hoặc được lợi (-)

    @Column(nullable = false)
    private LocalDate revenueDate;     // Ngày tính doanh thu (thường = ngày release tiền)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (revenueDate == null) {
            revenueDate = LocalDate.now();
        }
        if (amount == null) amount = BigDecimal.ZERO;
        if (feePlatform == null) feePlatform = BigDecimal.ZERO;
        if (feeShipping == null) feeShipping = BigDecimal.ZERO;
    }
}
