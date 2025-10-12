package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_wallet")
public class PlatformWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ====== PHÂN BIỆT CHỦ THỂ (AI SỞ HỮU VÍ) ======
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletOwnerType ownerType;   // PLATFORM / SHOP / CUSTOMER

    @Column
    private UUID ownerId;                // ID của chủ thể (nếu là shopId hoặc customerId)

    // ====== SỐ DƯ VÍ ======
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBalance = BigDecimal.ZERO;     // Tổng tiền hiện có trong ví

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal pendingBalance = BigDecimal.ZERO;   // Tiền đang bị hold

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal doneBalance = BigDecimal.ZERO;      // Tiền đã xác nhận hoàn tất

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal receivedTotal = BigDecimal.ZERO;    // Tổng tiền từng nhận qua ví

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal refundedTotal = BigDecimal.ZERO;    // Tổng tiền đã hoàn trả (refund)

    @Column(nullable = false, length = 10)
    private String currency = "VND";

    // ====== QUAN HỆ GIAO DỊCH ======
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlatformTransaction> transactions = new ArrayList<>();

    // ====== THỜI GIAN ======
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}


