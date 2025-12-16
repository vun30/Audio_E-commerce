package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.MoneyStatsResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MoneyStatsService {

    MoneyStatsResponse getMoneyStats(
            LocalDateTime from,
            LocalDateTime to,
            UUID storeId,
            UUID customerId,
            String storeTxnType,      // StoreWalletTransactionType name (optional)
            String customerTxnType,   // WalletTransactionType name (optional)
            String platformTxnType,   // TransactionType name (optional)
            String platformStatus     // TransactionStatus name (optional)
    );
}
