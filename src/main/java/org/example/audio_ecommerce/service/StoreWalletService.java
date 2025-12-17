package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.DepositTransferRequest;
import org.example.audio_ecommerce.dto.request.WithdrawDepositToDefaultRequest;
import org.example.audio_ecommerce.dto.request.WithdrawRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreWalletOverviewResponse;
import org.example.audio_ecommerce.dto.response.StoreWalletTransactionResponse;
import org.example.audio_ecommerce.entity.Enum.DebtComponentType;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
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

    public ResponseEntity<BaseResponse> getMyDebtComponents(
            DebtComponentType componentType,     // optional
            String status,                       // optional: UNPAID|PAID
            Boolean payableNowOnly,
            LocalDateTime from,                  // optional
            LocalDateTime to,                    // optional
            BigDecimal minAmount,                // optional
            BigDecimal maxAmount,                // optional
            String orderCode,                    // optional
            String ghnOrderCode,                 // optional
            int page,
            int size
    );

    StoreWalletOverviewResponse getMyWalletOverview();

    ResponseEntity<BaseResponse> payMyDebtFromDefaultBalance();

    ResponseEntity<BaseResponse> withdrawFromDefaultBalance(WithdrawRequest req);

    ResponseEntity<BaseResponse> transferDefaultToDeposit(DepositTransferRequest req);

    ResponseEntity<BaseResponse> withdrawDepositToDefault(WithdrawDepositToDefaultRequest req);

}

