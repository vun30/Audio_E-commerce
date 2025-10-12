package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.PlatformTransactionResponse;
import org.example.audio_ecommerce.dto.response.PlatformWalletResponse;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlatformWalletService {

    // Lấy tất cả ví
    List<PlatformWalletResponse> getAllWallets();

    // Lấy ví theo ownerId (shop hoặc customer)
    PlatformWalletResponse getWalletByOwner(UUID ownerId);

    // Lọc giao dịch
    List<PlatformTransactionResponse> filterTransactions(
        UUID storeId,
        UUID customerId,
        TransactionStatus status,
        TransactionType type,
        LocalDateTime from,
        LocalDateTime to
);
}
