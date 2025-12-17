package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.PlatformFee;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;
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
        // Kiểm tra xem đã có ví platform chưa
        boolean exists = walletRepository.findByOwnerType(WalletOwnerType.PLATFORM).stream().findFirst().isPresent();

        if (!exists) {
            // 1️⃣ Tạo ví platform mặc định
            PlatformWallet platformWallet = PlatformWallet.builder()
                    .ownerType(WalletOwnerType.PLATFORM)
                    .ownerId(null)
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

            // 2️⃣ Tạo transaction mặc định (log khởi tạo)
            PlatformTransaction defaultTxn = PlatformTransaction.builder()
                    .wallet(platformWallet)
                    .orderId(null)
                    .storeId(null)
                    .customerId(null)
                    .amount(BigDecimal.ZERO)
                    .type(TransactionType.INITIALIZE)
                    .status(TransactionStatus.DONE)
                    .description("Khởi tạo ví platform mặc định")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(defaultTxn);

            System.out.println("✅ Ví platform mặc định đã được tạo với transaction 0 VNĐ");
        }
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
