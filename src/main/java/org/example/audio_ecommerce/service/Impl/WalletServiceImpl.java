package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.WalletTxnRequest;
import org.example.audio_ecommerce.dto.response.WalletResponse;
import org.example.audio_ecommerce.dto.response.WalletTransactionResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.WalletRepository;
import org.example.audio_ecommerce.repository.WalletTransactionRepository;
import org.example.audio_ecommerce.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getByCustomer(UUID customerId) {
        Wallet w = walletRepo.findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return toWalletResponse(w);
    }

    @Override
    public WalletTransactionResponse deposit(UUID customerId, WalletTxnRequest req) {
        return doChangeBalance(customerId, req, WalletTransactionType.DEPOSIT);
    }

    @Override
    public WalletTransactionResponse withdraw(UUID customerId, WalletTxnRequest req) {
        return doChangeBalance(customerId, req, WalletTransactionType.WITHDRAW);
    }

    @Override
    public WalletTransactionResponse payment(UUID customerId, WalletTxnRequest req) {
        if (req.getOrderId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required for PAYMENT");
        // Idempotency: nếu đã có PAYMENT cho order này rồi -> trả về luôn
        var existed = txnRepo.findFirstByWallet_Customer_IdAndOrderIdAndTransactionTypeOrderByCreatedAtDesc(
                customerId, req.getOrderId(), WalletTransactionType.PAYMENT);
        if (existed.isPresent()) return toTxnResponse(existed.get());
        return doChangeBalance(customerId, req, WalletTransactionType.PAYMENT);
    }

    @Override
    public WalletTransactionResponse refund(UUID customerId, WalletTxnRequest req) {
        if (req.getOrderId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required for REFUND");
        return doChangeBalance(customerId, req, WalletTransactionType.REFUND);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> listTransactions(UUID customerId, Pageable pageable) {
        return txnRepo.findByWallet_Customer_IdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toTxnResponse);
    }

    // ===== Core =====
    private WalletTransactionResponse doChangeBalance(UUID customerId, WalletTxnRequest req,
                                                      WalletTransactionType type) {
        Wallet wallet = walletRepo.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getStatus() == WalletStatus.LOCKED)
            throw new ResponseStatusException(HttpStatus.LOCKED, "Wallet is locked");

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be > 0");
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after;

        switch (type) {
            case DEPOSIT, REFUND -> after = before.add(amount);
            case WITHDRAW, PAYMENT -> {
                if (before.compareTo(amount) < 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
                after = before.subtract(amount);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported type");
        }

        // cập nhật ví
        wallet.setBalance(after);
        wallet.setLastTransactionAt(LocalDateTime.now());

        // ghi giao dịch
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .status(WalletTransactionStatus.SUCCESS) // xử lý đồng bộ
                .description(req.getDescription())
                .balanceBefore(before)
                .balanceAfter(after)
                .orderId(req.getOrderId())
                .build();

        txnRepo.save(txn);
        return toTxnResponse(txn);
    }

    // ===== Mappers =====
    private WalletResponse toWalletResponse(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .customerId(w.getCustomer().getId())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .status(w.getStatus().name())
                .lastTransactionAt(w.getLastTransactionAt())
                .build();
    }

    private WalletTransactionResponse toTxnResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getId())
                .orderId(t.getOrderId())
                .type(t.getTransactionType().name())
                .status(t.getStatus().name())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
