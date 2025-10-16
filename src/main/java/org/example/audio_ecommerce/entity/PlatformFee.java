package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "platform_fees")
public class PlatformFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fee_id", columnDefinition = "CHAR(36)")
    private UUID feeId;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage; // ví dụ 5.00 = 5%

    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate; // ngày có hiệu lực

    private String description; // mô tả (ví dụ: "Phí áp dụng cho Q4/2025")

    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
