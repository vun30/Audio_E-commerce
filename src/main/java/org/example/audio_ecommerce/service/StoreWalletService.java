package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreWalletTransactionResponse;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StoreWalletService {
    ResponseEntity<BaseResponse> getMyWallet();

    ResponseEntity<BaseResponse> getMyWalletTransactions(int page, int size, String type);

    Page<StoreWalletTransactionResponse> filterTransactions(
        UUID walletId,
        LocalDateTime from,
        LocalDateTime to,
        StoreWalletTransactionType type,
        UUID transactionId,
        Pageable pageable
    );

    UUID resolveWalletIdForCurrentUser();
}

