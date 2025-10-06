package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.WalletTxnRequest;
import org.example.audio_ecommerce.dto.response.WalletResponse;
import org.example.audio_ecommerce.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WalletService {
    WalletResponse getByCustomer(UUID customerId);

    WalletTransactionResponse deposit(UUID customerId, WalletTxnRequest req);
    WalletTransactionResponse withdraw(UUID customerId, WalletTxnRequest req);
    WalletTransactionResponse payment(UUID customerId, WalletTxnRequest req);
    WalletTransactionResponse refund(UUID customerId, WalletTxnRequest req);

    Page<WalletTransactionResponse> listTransactions(UUID customerId, Pageable pageable);
}
