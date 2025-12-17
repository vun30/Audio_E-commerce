package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerWithdrawCreateRequest;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.entity.Wallet;
import org.example.audio_ecommerce.entity.WalletTransaction;
import org.example.audio_ecommerce.repository.CustomerWithdrawRequestRepository;
import org.example.audio_ecommerce.repository.WithdrawProofRepository;
import org.example.audio_ecommerce.repository.WalletRepository;
import org.example.audio_ecommerce.repository.WalletTransactionRepository;
import org.example.audio_ecommerce.service.CustomerWithdrawService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerWithdrawServiceImpl implements CustomerWithdrawService {

    private final WalletRepository walletRepo; // wallet
    private final CustomerWithdrawRequestRepository withdrawRepo;
    private final WalletTransactionRepository txnRepo;
    private final WithdrawProofRepository proofRepo;

    @Override
    @Transactional
    public CustomerWithdrawRequest create(UUID customerId, CustomerWithdrawCreateRequest req) {

        Wallet wallet = walletRepo.findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        BigDecimal amount = req.getAmount()
                .setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be > 0");
        }

        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal pending = wallet.getPendingBalance() != null ? wallet.getPendingBalance() : BigDecimal.ZERO;

        if (balance.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // 1) Hold tiền: balance giảm, pending tăng
        BigDecimal before = balance;
        BigDecimal after = before.subtract(amount);

        wallet.setBalance(after);
        wallet.setPendingBalance(pending.add(amount));
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        // 2) Tạo request PENDING
        CustomerWithdrawRequest wr = CustomerWithdrawRequest.builder()
                .customerId(customerId)
                .walletId(wallet.getId())
                .amount(amount)
                .bankCode(req.getBankCode())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountName(req.getAccountName())
                .status(WithdrawRequestStatus.PENDING)
                .adminNote(null)
                .payoutRef(null)
                .build();
        withdrawRepo.save(wr);

        // 3) Log wallet transaction PENDING
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.WITHDRAW_REQUEST)
                .status(WalletTransactionStatus.PENDING)
                .description("Customer withdraw request id=" + wr.getId())
                .balanceBefore(before)
                .balanceAfter(after)
                .orderId(null)
                .externalRef("CWR:" + wr.getId())
                .build();
        txnRepo.save(txn);

        return wr;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerWithdrawResponse> customerList(UUID customerId, WithdrawRequestStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CustomerWithdrawRequest> data = (status == null)
                ? withdrawRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                : withdrawRepo.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, status, pageable);

        return data.map(this::toResponseNoProofs);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerWithdrawResponse customerGet(UUID customerId, UUID withdrawRequestId) {

        CustomerWithdrawRequest wr = withdrawRepo.findById(withdrawRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw request not found"));

        if (!wr.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        CustomerWithdrawResponse res = toResponseNoProofs(wr);
        res.setProofUrls(
                proofRepo.findByWithdrawRequestIdOrderByCreatedAtDesc(withdrawRequestId)
                        .stream()
                        .map(p -> p.getFileUrl())
                        .collect(Collectors.toList())
        );
        return res;
    }

    private CustomerWithdrawResponse toResponseNoProofs(CustomerWithdrawRequest wr) {
        CustomerWithdrawResponse res = new CustomerWithdrawResponse();
        res.setId(wr.getId());
        res.setCustomerId(wr.getCustomerId());
        res.setAmount(wr.getAmount());
        res.setStatus(wr.getStatus());

        res.setBankCode(wr.getBankCode());
        res.setBankName(wr.getBankName());
        res.setAccountNumber(wr.getAccountNumber());
        res.setAccountName(wr.getAccountName());

        res.setAdminNote(wr.getAdminNote());
        res.setPayoutRef(wr.getPayoutRef());

        res.setCreatedAt(wr.getCreatedAt());
        res.setUpdatedAt(wr.getUpdatedAt());
        return res;
    }
}
