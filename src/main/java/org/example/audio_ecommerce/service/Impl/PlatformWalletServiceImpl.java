package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PlatformTransactionResponse;
import org.example.audio_ecommerce.dto.response.PlatformWalletOverviewResponse;
import org.example.audio_ecommerce.dto.response.PlatformWalletResponse;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.repository.PlatformWalletRepository;
import org.example.audio_ecommerce.service.PlatformWalletService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformWalletServiceImpl implements PlatformWalletService {

    private final PlatformWalletRepository walletRepository;
    private final PlatformTransactionRepository transactionRepository;

    // ====== Mapper nội bộ ======
    private PlatformWalletResponse mapToWalletResponse(PlatformWallet wallet, boolean includeTransactions) {
        PlatformWalletResponse.PlatformWalletResponseBuilder builder = PlatformWalletResponse.builder()
                .id(wallet.getId())
                .ownerType(wallet.getOwnerType())
                .ownerId(wallet.getOwnerId())
                .totalBalance(wallet.getTotalBalance())
                .pendingBalance(wallet.getPendingBalance())
                .doneBalance(wallet.getDoneBalance())
                .receivedTotal(wallet.getReceivedTotal())
                .refundedTotal(wallet.getRefundedTotal())
                .currency(wallet.getCurrency())
                .createdAt(wallet.getCreatedAt());

        if (includeTransactions && wallet.getTransactions() != null) {
            List<PlatformTransactionResponse> txList = wallet.getTransactions()
                    .stream()
                    .map(this::mapToTransactionResponse)
                    .collect(Collectors.toList());
            builder.transactions(txList);
        }

        return builder.build();
    }

    private PlatformTransactionResponse mapToTransactionResponse(PlatformTransaction tx) {
        return PlatformTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWallet().getId())
                .orderId(tx.getOrderId())
                .storeId(tx.getStoreId())
                .customerId(tx.getCustomerId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    // ====== Service logic ======

    @Override
    public List<PlatformWalletResponse> getAllWallets() {
        return walletRepository.findAll()
                .stream()
                .map(wallet -> mapToWalletResponse(wallet, false))
                .collect(Collectors.toList());
    }

    @Override
    public PlatformWalletResponse getWalletByOwner(UUID ownerId) {
        PlatformWallet wallet = walletRepository.findAll()
                .stream()
                .filter(w -> w.getOwnerId() != null && w.getOwnerId().equals(ownerId))
                .findFirst()
                .orElse(null);

        return wallet != null ? mapToWalletResponse(wallet, true) : null;
    }

    @Override
    public List<PlatformTransactionResponse> filterTransactions(
            UUID storeId,
            UUID customerId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return transactionRepository.filterTransactions(storeId, customerId, status, type, from, to)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlatformWalletResponse getPlatformWallet() {
        PlatformWallet wallet = walletRepository.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElse(null); // hoặc throw nếu bạn muốn

        return wallet != null ? mapToWalletResponse(wallet, true) : null;
    }

    @Override
    public PlatformWalletOverviewResponse getPlatformWalletOverview() {
        // Lấy platform wallet
        PlatformWallet wallet = walletRepository.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new RuntimeException("Platform wallet not found"));

        // Đếm số lượng order pending và done
        Long pendingOrderCount = transactionRepository.countByStatusAndType(
                TransactionStatus.PENDING,
                TransactionType.HOLD
        );

        Long doneOrderCount = transactionRepository.countByStatusAndType(
                TransactionStatus.DONE,
                TransactionType.HOLD
        );

        // Tạo summary
        String summary = String.format(
                "Platform wallet healthy: %s VND cash, %s VND pending",
                wallet.getCashBalance().setScale(0, java.math.RoundingMode.DOWN),
                wallet.getPendingBalance().setScale(0, java.math.RoundingMode.DOWN)
        );

        // Map sang DTO
        return PlatformWalletOverviewResponse.builder()
                .totalCustomerDeposit(wallet.getReceivedTotal())  // ✅ Tổng tiền nạp
                .pendingBalance(wallet.getPendingBalance())
                .doneBalance(wallet.getDoneBalance())
                .refundedTotal(wallet.getRefundedTotal())
                .commissionBalance(wallet.getCommissionBalance())
                .cashBalance(wallet.getCashBalance())
                .totalBalance(wallet.getTotalBalance())
                .pendingOrderCount(pendingOrderCount)
                .doneOrderCount(doneOrderCount)
                .lastUpdatedAt(wallet.getUpdatedAt())
                .summary(summary)
                .build();
    }

}
