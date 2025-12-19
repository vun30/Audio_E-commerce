package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerWithdrawCreateRequest;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
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
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformTransactionRepository platformTransactionRepository;

    @Override
    @Transactional
    public CustomerWithdrawRequest create(UUID customerId, CustomerWithdrawCreateRequest req) {

        Wallet wallet = walletRepo.findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be > 0");
        }

        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // 1) Trừ tiền luôn (không hold pending)
        BigDecimal before = balance;
        BigDecimal after = before.subtract(amount);

        wallet.setBalance(after);
        // nếu bạn vẫn muốn giữ pendingBalance thì set về 0/giữ nguyên, nhưng KHÔNG tăng nữa
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        // 2) Tạo request DONE (không cần admin duyệt)
        CustomerWithdrawRequest wr = CustomerWithdrawRequest.builder()
                .customerId(customerId)
                .walletId(wallet.getId())
                .amount(amount)
                .bankCode(req.getBankCode())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountName(req.getAccountName())
                .status(WithdrawRequestStatus.PAID)     // ✅ DONE luôn
                .adminNote("Auto-approved")             // optional
                .payoutRef("AUTO:" + UUID.randomUUID()) // optional, để trace
                .build();
        withdrawRepo.save(wr);

        // ===== PLATFORM CASH OUT (CUSTOMER WITHDRAW) =====
        PlatformWallet platform = platformWalletRepository.getPlatformMainWallet();

        BigDecimal cashBefore = platform.getCashBalance() != null ? platform.getCashBalance() : BigDecimal.ZERO;
        BigDecimal cashAfter  = cashBefore.subtract(amount);

        platform.setCashBalance(cashAfter);
        platform.setRefundedTotal(
                (platform.getRefundedTotal() != null ? platform.getRefundedTotal() : BigDecimal.ZERO)
                        .add(amount)
        );
        platformWalletRepository.save(platform);

        // Lưu platform_transaction (bucket=CASH)
        PlatformTransaction pTxn = PlatformTransaction.builder()
                .wallet(platform)
                .orderId(null)
                .storeId(null)
                .customerId(customerId)
                .amount(amount)
                .type(TransactionType.WITHDRAW)                    // đổi đúng enum của bạn
                .status(TransactionStatus.DONE)
                .description("Customer withdraw (auto) withdrawReqId=" + wr.getId())
                .idempotencyKey("PAYOUT:CUS_WITHDRAW:" + wr.getId()) // unique
                .channel(PaymentChannel.BANK_TRANSFER)             // hoặc INTERNAL nếu bạn chưa payout thật
                .externalRefId(wr.getPayoutRef())                  // nếu payoutRef có
                .externalRefCode("CWR:" + wr.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .bucket(WalletBucket.CASH)
                .direction(TxDirection.OUT)
                .balanceBefore(cashBefore)
                .balanceAfter(cashAfter)
                .build();

        platformTransactionRepository.save(pTxn);


        // 3) Log wallet transaction DONE
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.WITHDRAW_REQUEST)
                .status(WalletTransactionStatus.SUCCESS) // ✅ DONE luôn
                .description("Customer withdraw (auto) id=" + wr.getId())
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
