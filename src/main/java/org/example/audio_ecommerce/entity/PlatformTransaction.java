package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_transaction")
public class PlatformTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ví platform trung gian chứa giao dịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private PlatformWallet wallet;

    // Giao dịch liên quan tới đơn hàng nào
    @Column(nullable = true)
    private UUID orderId;

    // ID cửa hàng nhận tiền (nếu là giao dịch với shop)
    @Column
    private UUID storeId;

    // ID khách hàng nhận tiền (nếu là giao dịch refund)
    @Column
    private UUID customerId;

    // Số tiền giao dịch
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // Loại giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    // Trạng thái giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    // Ghi chú chi tiết
    @Column(length = 255)
    private String description;

    // Thời điểm tạo và cập nhật
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();


}
