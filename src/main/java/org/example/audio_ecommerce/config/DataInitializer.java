package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.PlatformFee;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.repository.PlatformWalletRepository;
import org.example.audio_ecommerce.repository.PlatformFeeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final PlatformWalletRepository walletRepository;
    private final PlatformTransactionRepository transactionRepository;
    private final PlatformFeeRepository feeRepository;

    @PostConstruct
    public void initDefaultPlatformWallet() {

        boolean exists = walletRepository.findByOwnerType(WalletOwnerType.PLATFORM)
                .stream()
                .findFirst()
                .isPresent();

        if (exists) {
            return;
        }

        // =========================
        // 1️⃣ CREATE PLATFORM WALLET
        // =========================
        PlatformWallet platformWallet = PlatformWallet.builder()
                .ownerType(WalletOwnerType.PLATFORM)
                .ownerId(null)
                .cashBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .doneBalance(BigDecimal.ZERO)
                .receivedTotal(BigDecimal.ZERO)
                .refundedTotal(BigDecimal.ZERO)
                .currency("VND")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        walletRepository.save(platformWallet);

        // =========================
        // 2️⃣ CREATE INIT TRANSACTION (FLAT TRAN DEFAULT)
        // =========================
        BigDecimal before = platformWallet.getCashBalance(); // 0
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal after = before.add(amount);               // 0

        PlatformTransaction initTxn = PlatformTransaction.builder()
                // ===== FK =====
                .wallet(platformWallet)

                // ===== CORE =====
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)

                // ===== LEDGER META =====
                .type(TransactionType.INITIALIZE)
                .status(TransactionStatus.DONE)
                .channel(PaymentChannel.INTERNAL)   // ✅ FIX
                .bucket(WalletBucket.CASH)           // ✅ FIX
                .direction(TxDirection.IN)            // ✅ FIX

                // ===== FINANCIAL DEFAULTS =====
                .commissionAmount(BigDecimal.ZERO)   // ✅ FIX
                .commissionRate(BigDecimal.ZERO)     // ✅ FIX
                .debtDeducted(BigDecimal.ZERO)       // ✅ FIX

                // ===== OPTIONAL (SAFE DEFAULT) =====
                .itemAmount(BigDecimal.ZERO)
                .shipCustomerPaid(BigDecimal.ZERO)
                .shipReal(BigDecimal.ZERO)
                .shipDiffChargeStore(BigDecimal.ZERO)
                .payoutGross(BigDecimal.ZERO)
                .payoutNet(BigDecimal.ZERO)

                // ===== META =====
                .description("INIT platform wallet ledger")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())

                .build();

        transactionRepository.save(initTxn);

        System.out.println("✅ Platform wallet + flat transaction INIT đã được khởi tạo");
    }
    @PostConstruct
    public void initDefaultPlatformFee() {
        // Kiểm tra xem đã có phí nền tảng nào hoạt động chưa
        boolean exists = feeRepository.findByIsActiveTrue().isPresent();

        if (!exists) {
            // 🏪 Tạo phí nền tảng mặc định 5%
            PlatformFee defaultFee = PlatformFee.builder()
                    .percentage(BigDecimal.valueOf(5.00)) // 5%
                    .effectiveDate(LocalDateTime.now())
                    .description("Phí nền tảng mặc định 5%")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            feeRepository.save(defaultFee);

            System.out.println("✅ Phí nền tảng mặc định (5%) đã được tạo và kích hoạt");
        }
    }
}
