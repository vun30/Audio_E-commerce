package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "customer_withdraw_requests",
        indexes = {
                @Index(name = "idx_cwr_customer", columnList = "customer_id"),
                @Index(name = "idx_cwr_status", columnList = "status"),
                @Index(name = "idx_cwr_created", columnList = "created_at")
        }
)
public class CustomerWithdrawRequest extends BaseEntity {
    //entity này để lưu các yêu cầu rút tiền của khách hàng từ ví
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID customerId;

    @Column(name = "wallet_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID walletId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // Bank info
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private WithdrawRequestStatus status;

    @Column(length = 512)
    private String adminNote;

    @Column(name = "payout_ref", length = 128)
    private String payoutRef;
}
