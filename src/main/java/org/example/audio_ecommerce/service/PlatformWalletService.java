package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlatformWalletService {

    List<PlatformWalletResponse> getAllWallets();

    PlatformWalletResponse getWalletByOwner(UUID ownerId);

    // giữ cũ nếu chỗ khác đang gọi
    List<PlatformTransactionResponse> filterTransactions(
            UUID storeId,
            UUID customerId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to
    );

    // ✅ mới: filter transaction cho ví tổng/flat wallet duy nhất (paging + filter đầy đủ)
    Page<PlatformTransactionResponse> filterFlatWalletTransactions(
            UUID storeId,
            UUID customerId,
            UUID orderId,
            UUID payoutRequestId,

            TransactionStatus status,
            TransactionType type,
            WalletBucket bucket,
            TxDirection direction,
            PaymentChannel channel,

            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    PlatformWalletResponse getPlatformWallet();

    PlatformWalletOverviewResponse getPlatformWalletOverview();

    Page<PlatformTransactionResponse> getFlatWalletTransactions(
            TransactionType type,              // null = tất cả
            TransactionStatus status,           // null = tất cả (tuỳ bạn giữ)
            LocalDateTime from,                // null = không chặn dưới
            LocalDateTime to,                  // null = không chặn trên
            Pageable pageable
    );

    FlatGhnDebtOverviewResponse getFlatGhnDebtOverview(LocalDateTime from, LocalDateTime to);

    FlatGhnOverviewResponse getFlatGhnOverview(LocalDateTime from, LocalDateTime to);

    PlatformRevenueOverviewResponse getPlatformRevenueOverview();

    List<PlatformGrowthChartPoint> getPlatformGrowthChartByMonth();

    List<PlatformGrowthChartPoint> getPlatformGrowthChartByYear();

//    FlatGhnShipFeeOverviewResponse getFlatGhnShipFeeOverview(
//            LocalDateTime from,
//            LocalDateTime to
//    );
//
//    FlatStoreDebtSummaryResponse getFlatStoreDebtSummary(LocalDateTime from, LocalDateTime to);
//
//    BigDecimal getTotalCustomerShipPaid(LocalDateTime from, LocalDateTime to);
//
//    ReturnShipFeeSummaryResponse getReturnShipFeeSummary(LocalDateTime from, LocalDateTime to);
//
//
}
