package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.MoneyStatsResponse;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.repository.PlatformWalletRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.repository.StoreWalletTransactionRepository;
import org.example.audio_ecommerce.repository.WalletRepository;
import org.example.audio_ecommerce.repository.WalletTransactionRepository;
import org.example.audio_ecommerce.service.MoneyStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoneyStatsServiceImpl implements MoneyStatsService {

    private final PlatformWalletRepository platformWalletRepo;
    private final StoreWalletRepository storeWalletRepo;
    private final WalletRepository walletRepo;

    private final PlatformTransactionRepository platformTxnRepo;
    private final StoreWalletTransactionRepository storeWalletTxnRepo;
    private final WalletTransactionRepository walletTxnRepo;

    @Override
    public MoneyStatsResponse getMoneyStats(
            LocalDateTime from,
            LocalDateTime to,
            UUID storeId,
            UUID customerId,
            String storeTxnType,
            String customerTxnType,
            String platformTxnType,
            String platformStatus
    ) {
        // ===== 1) Snapshot PLATFORM =====
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(org.example.audio_ecommerce.entity.Enum.WalletOwnerType.PLATFORM)
                .orElse(null);

        MoneyStatsResponse.PlatformMoney platformMoney = MoneyStatsResponse.PlatformMoney.builder()
                .totalBalance(nz(plat != null ? plat.getTotalBalance() : null))
                .pendingBalance(nz(plat != null ? plat.getPendingBalance() : null))
                .doneBalance(nz(plat != null ? plat.getDoneBalance() : null))
                .receivedTotal(nz(plat != null ? plat.getReceivedTotal() : null))
                .refundedTotal(nz(plat != null ? plat.getRefundedTotal() : null))
                .commissionBalance(nz(plat != null ? plat.getCommissionBalance() : null))
                .build();

        // ===== 2) Snapshot STORE =====
        // - Nếu storeId != null: lấy 1 store_wallet
        // - Nếu storeId == null: SUM toàn bộ store_wallet
        MoneyStatsResponse.StoreMoney storeMoney;
        if (storeId != null) {
            var sw = storeWalletRepo.findByStore_StoreId(storeId).orElse(null);
            storeMoney = MoneyStatsResponse.StoreMoney.builder()
                    .storeId(storeId)
                    .availableBalance(nz(sw != null ? sw.getAvailableBalance() : null))
                    .pendingBalance(nz(sw != null ? sw.getPendingBalance() : null))
                    .depositBalance(nz(sw != null ? sw.getDepositBalance() : null))
                    .totalRevenue(nz(sw != null ? sw.getTotalRevenue() : null))
                    .build();
        } else {
            // sum all store wallets
            var all = storeWalletRepo.findAll();
            BigDecimal avail = BigDecimal.ZERO, pend = BigDecimal.ZERO, depo = BigDecimal.ZERO, rev = BigDecimal.ZERO;
            for (var sw : all) {
                avail = avail.add(nz(sw.getAvailableBalance()));
                pend  = pend.add(nz(sw.getPendingBalance()));
                depo  = depo.add(nz(sw.getDepositBalance()));
                rev   = rev.add(nz(sw.getTotalRevenue()));
            }
            storeMoney = MoneyStatsResponse.StoreMoney.builder()
                    .storeId(null)
                    .availableBalance(avail)
                    .pendingBalance(pend)
                    .depositBalance(depo)
                    .totalRevenue(rev)
                    .build();
        }

        // ===== 3) Snapshot CUSTOMER =====
        MoneyStatsResponse.CustomerMoney customerMoney;
        if (customerId != null) {
            var cw = walletRepo.findByCustomer_Id(customerId).orElse(null);
            customerMoney = MoneyStatsResponse.CustomerMoney.builder()
                    .customerId(customerId)
                    .balance(nz(cw != null ? cw.getBalance() : null))
                    .build();
        } else {
            // sum all customer wallet balances
            var all = walletRepo.findAll();
            BigDecimal total = BigDecimal.ZERO;
            for (var w : all) total = total.add(nz(w.getBalance()));
            customerMoney = MoneyStatsResponse.CustomerMoney.builder()
                    .customerId(null)
                    .balance(total)
                    .build();
        }

        // ===== 4) Sum theo TYPE (optional) =====
        var typeSummaries = new ArrayList<MoneyStatsResponse.TypeSummary>();

        // STORE type sum
        if (storeTxnType != null && !storeTxnType.isBlank()) {
            StoreWalletTransactionType t = StoreWalletTransactionType.valueOf(storeTxnType.trim().toUpperCase());
            BigDecimal sum = storeWalletTxnRepo.sumAmountByTypeAndFilter(storeId, t, from, to);
            typeSummaries.add(MoneyStatsResponse.TypeSummary.builder()
                    .walletSystem("STORE")
                    .type(t.name())
                    .totalAmount(nz(sum))
                    .build());
        }

        // CUSTOMER type sum
        if (customerTxnType != null && !customerTxnType.isBlank()) {
            WalletTransactionType t = WalletTransactionType.valueOf(customerTxnType.trim().toUpperCase());
            BigDecimal sum = walletTxnRepo.sumAmountByTypeAndFilter(customerId, t, from, to);
            typeSummaries.add(MoneyStatsResponse.TypeSummary.builder()
                    .walletSystem("CUSTOMER")
                    .type(t.name())
                    .totalAmount(nz(sum))
                    .build());
        }

        // PLATFORM type sum
        if (platformTxnType != null && !platformTxnType.isBlank()) {
            TransactionType t = TransactionType.valueOf(platformTxnType.trim().toUpperCase());
            TransactionStatus st = (platformStatus == null || platformStatus.isBlank())
                    ? null
                    : TransactionStatus.valueOf(platformStatus.trim().toUpperCase());

            BigDecimal sum = platformTxnRepo.sumAmountByTypeAndFilter(t, st, from, to);
            typeSummaries.add(MoneyStatsResponse.TypeSummary.builder()
                    .walletSystem("PLATFORM")
                    .type(t.name())
                    .totalAmount(nz(sum))
                    .build());
        }

        return MoneyStatsResponse.builder()
                .currency("VND")
                .from(from)
                .to(to)
                .platform(platformMoney)
                .store(storeMoney)
                .customer(customerMoney)
                .typeSummaries(typeSummaries)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
