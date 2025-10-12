package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "store_wallet_transactions")
public class StoreWalletTransaction {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "transaction_id", columnDefinition = "CHAR(36)")
    private UUID transactionId;

    // 🔗 Liên kết với ví cửa hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, columnDefinition = "CHAR(36)")
    private StoreWallet wallet;

    // 📄 Loại giao dịch: DEPOSIT, WITHDRAW, PENDING_HOLD, RELEASE_PENDING, REFUND, ...
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private StoreWalletTransactionType type;

    // 💰 Số tiền thay đổi
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    // 💸 Số dư sau giao dịch
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    // 🧾 Mô tả giao dịch
    @Column(length = 255)
    private String description;

    // 📦 ID đơn hàng liên quan (nếu có)
    @Column(name = "order_id", columnDefinition = "CHAR(36)")
    private UUID orderId;

    private LocalDateTime createdAt;
}
