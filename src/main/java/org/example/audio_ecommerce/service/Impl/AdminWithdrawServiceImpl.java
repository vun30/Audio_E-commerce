package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AdminDecisionRequest;
import org.example.audio_ecommerce.dto.request.AdminWithdrawMarkPaidRequest;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.entity.Wallet;
import org.example.audio_ecommerce.entity.WalletTransaction;
import org.example.audio_ecommerce.entity.WithdrawProof;
import org.example.audio_ecommerce.repository.CustomerWithdrawRequestRepository;
import org.example.audio_ecommerce.repository.WithdrawProofRepository;
import org.example.audio_ecommerce.repository.WalletRepository;
import org.example.audio_ecommerce.repository.WalletTransactionRepository;
import org.example.audio_ecommerce.service.AdminWithdrawService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWithdrawServiceImpl implements AdminWithdrawService {

    private final CustomerWithdrawRequestRepository withdrawRepo;
    private final WithdrawProofRepository proofRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerWithdrawResponse> adminList(WithdrawRequestStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CustomerWithdrawRequest> data = (status == null)
                ? withdrawRepo.findAllByOrderByCreatedAtDesc(pageable)
                : withdrawRepo.findByStatusOrderByCreatedAtDesc(status, pageable);

        return data.map(this::toResponseNoProofs);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerWithdrawResponse adminGet(UUID withdrawRequestId) {
        CustomerWithdrawRequest wr = withdrawRepo.findById(withdrawRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw request not found"));

        CustomerWithdrawResponse res = toResponseNoProofs(wr);
        res.setProofUrls(
                proofRepo.findByWithdrawRequestIdOrderByCreatedAtDesc(withdrawRequestId)
                        .stream()
                        .map(WithdrawProof::getFileUrl)
                        .collect(Collectors.toList())
        );
        return res;
    }

    @Override
    @Transactional
    public CustomerWithdrawResponse approve(UUID withdrawRequestId, AdminDecisionRequest req) {

        CustomerWithdrawRequest wr = withdrawRepo.findById(withdrawRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw request not found"));

        if (wr.getStatus() != WithdrawRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING can be approved");
        }

        wr.setStatus(WithdrawRequestStatus.APPROVED);
        withdrawRepo.save(wr);

        return adminGet(withdrawRequestId);
    }

    @Override
    @Transactional
    public CustomerWithdrawResponse reject(UUID withdrawRequestId, AdminDecisionRequest req) {

        CustomerWithdrawRequest wr = withdrawRepo.findById(withdrawRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw request not found"));

        if (wr.getStatus() != WithdrawRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING can be rejected");
        }

        Wallet wallet = walletRepo.findById(wr.getWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        BigDecimal amount = wr.getAmount();

        BigDecimal balBefore = nz(wallet.getBalance());
        BigDecimal pendBefore = nz(wallet.getPendingBalance());

        if (pendBefore.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pending balance is invalid");
        }

        // release hold: pending giảm, balance tăng
        wallet.setPendingBalance(pendBefore.subtract(amount));
        wallet.setBalance(balBefore.add(amount));
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        wr.setStatus(WithdrawRequestStatus.REJECTED);
        withdrawRepo.save(wr);

        txnRepo.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.WITHDRAW_RELEASE)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Reject withdraw request id=" + wr.getId())
                .balanceBefore(balBefore)
                .balanceAfter(wallet.getBalance())
                .orderId(null)
                .externalRef("CWR:" + wr.getId())
                .build());

        return adminGet(withdrawRequestId);
    }

    @Override
    @Transactional
    public CustomerWithdrawResponse markPaid(UUID withdrawRequestId, AdminWithdrawMarkPaidRequest req) {

        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (req.getProofUrls() == null || req.getProofUrls().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "proofUrls is required");
        }

        CustomerWithdrawRequest wr = withdrawRepo.findById(withdrawRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw request not found"));

        if (wr.getStatus() != WithdrawRequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only APPROVED can be marked PAID");
        }

        // 1) Lưu proof URLs (dedupe)
        List<String> urls = req.getProofUrls().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (urls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "proofUrls cannot be blank");
        }

        // Optional: nếu muốn tránh spam, giới hạn số ảnh
        if (urls.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max 10 proofUrls");
        }

        // Lưu proof record
        for (String url : urls) {
            proofRepo.save(WithdrawProof.builder()
                    .withdrawRequestId(withdrawRequestId)
                    .fileUrl(url)
                    .fileName(null)
                    .fileType(null)
                    .note(null)
                    .build());
        }

        // 2) Xuất tiền khỏi hold (pending giảm)
        Wallet wallet = walletRepo.findById(wr.getWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        BigDecimal amount = wr.getAmount();
        BigDecimal pendBefore = nz(wallet.getPendingBalance());

        if (pendBefore.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pending balance is invalid");
        }

        wallet.setPendingBalance(pendBefore.subtract(amount));
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        // 3) Update request -> PAID
        wr.setStatus(WithdrawRequestStatus.PAID);
        wr.setPayoutRef(req.getPayoutRef());
        if (req.getNote() != null && !req.getNote().isBlank()) {
            wr.setAdminNote(req.getNote());
        }
        withdrawRepo.save(wr);

        // 4) Log payout transaction
        BigDecimal bal = nz(wallet.getBalance());
        txnRepo.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.WITHDRAW_PAYOUT)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Paid withdraw request id=" + wr.getId() + ", payoutRef=" + req.getPayoutRef())
                .balanceBefore(bal)
                .balanceAfter(bal) // balance không đổi ở bước PAID
                .orderId(null)
                .externalRef(req.getPayoutRef())
                .build());

        return adminGet(withdrawRequestId);
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

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
