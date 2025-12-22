package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.repository.projection.FlatOrderAgg2;
import org.example.audio_ecommerce.service.Projection.FlatDebtOrderRow;
import org.example.audio_ecommerce.service.PlatformWalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformWalletServiceImpl implements PlatformWalletService {

    private final PlatformWalletRepository walletRepository;
    private final PlatformTransactionRepository transactionRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreOrderItemRepository storeOrderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;

    // ====== Mapper nội bộ ======
    private PlatformWalletResponse mapToWalletResponse(PlatformWallet wallet, boolean includeTransactions) {
        PlatformWalletResponse.PlatformWalletResponseBuilder builder = PlatformWalletResponse.builder()
                .id(wallet.getId())
                .ownerType(wallet.getOwnerType())
                .ownerId(wallet.getOwnerId())
                .totalBalance(wallet.getTotalBalance())
                .pendingBalance(wallet.getPendingBalance())
                .doneBalance(wallet.getDoneBalance())
                .receivedTotal(wallet.getReceivedTotal())
                .refundedTotal(wallet.getRefundedTotal())
                .currency(wallet.getCurrency())
                .createdAt(wallet.getCreatedAt());

        // ⚠️ platform wallet có thể có rất nhiều transaction => KHÔNG khuyên load kiểu này
        if (includeTransactions && wallet.getTransactions() != null) {
            List<PlatformTransactionResponse> txList = wallet.getTransactions()
                    .stream()
                    .map(this::mapToTransactionResponse)
                    .collect(Collectors.toList());
            builder.transactions(txList);
        }

        return builder.build();
    }

    private PlatformTransactionResponse mapToTransactionResponse(PlatformTransaction tx) {
        return PlatformTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWallet() != null ? tx.getWallet().getId() : null)

                .orderId(tx.getOrderId())
                .storeId(tx.getStoreId())
                .customerId(tx.getCustomerId())

                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .channel(tx.getChannel())
                .bucket(tx.getBucket())
                .direction(tx.getDirection())

                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())

                .idempotencyKey(tx.getIdempotencyKey())
                .externalRefId(tx.getExternalRefId())
                .externalRefCode(tx.getExternalRefCode())

                .itemAmount(tx.getItemAmount())
                .shipCustomerPaid(tx.getShipCustomerPaid())
                .shipReal(tx.getShipReal())
                .shipDiffChargeStore(tx.getShipDiffChargeStore())
                .commissionAmount(tx.getCommissionAmount())
                .commissionRate(tx.getCommissionRate())

                .payoutRequestId(tx.getPayoutRequestId())
                .payoutGross(tx.getPayoutGross())
                .debtDeducted(tx.getDebtDeducted())
                .payoutNet(tx.getPayoutNet())

                .description(tx.getDescription())
                .metadataJson(tx.getMetadataJson())

                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }

    // ====== Service logic ======

    @Override
    public List<PlatformWalletResponse> getAllWallets() {
        return walletRepository.findAll()
                .stream()
                .map(wallet -> mapToWalletResponse(wallet, false))
                .collect(Collectors.toList());
    }

    @Override
    public PlatformWalletResponse getWalletByOwner(UUID ownerId) {
        // ⚠️ đang findAll rồi filter => chậm. Nếu có thể, tạo query findFirstByOwnerId(ownerId)
        PlatformWallet wallet = walletRepository.findAll()
                .stream()
                .filter(w -> w.getOwnerId() != null && w.getOwnerId().equals(ownerId))
                .findFirst()
                .orElse(null);

        return wallet != null ? mapToWalletResponse(wallet, true) : null;
    }

    @Override
    public List<PlatformTransactionResponse> filterTransactions(
            UUID storeId,
            UUID customerId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return transactionRepository.filterTransactions(storeId, customerId, status, type, from, to)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    // ✅ IMPLEMENT ĐÚNG THEO INTERFACE MỚI (KHÔNG walletId)
    @Override
    public Page<PlatformTransactionResponse> filterFlatWalletTransactions(
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
    ) {
        return transactionRepository.filterFlatWalletTransactions(
                        storeId,
                        customerId,
                        orderId,
                        payoutRequestId,
                        status,
                        type,
                        bucket,
                        direction,
                        channel,
                        from,
                        to,
                        pageable
                )
                .map(this::mapToTransactionResponse);
    }

    @Override
    public PlatformWalletResponse getPlatformWallet() {
        PlatformWallet wallet = walletRepository.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElse(null);

        // ✅ chỉ trả overview, không nhét transactions vào (vì rất nhiều tran)
        return wallet != null ? mapToWalletResponse(wallet, false) : null;
    }

    @Override
    public PlatformWalletOverviewResponse getPlatformWalletOverview() {
        PlatformWallet wallet = walletRepository.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new RuntimeException("Platform wallet not found"));

        Long pendingOrderCount = transactionRepository.countByStatusAndType(
                TransactionStatus.PENDING,
                TransactionType.HOLD
        );

        Long doneOrderCount = transactionRepository.countByStatusAndType(
                TransactionStatus.DONE,
                TransactionType.HOLD
        );

        String summary = String.format(
                "Platform wallet healthy: %s VND cash, %s VND pending",
                wallet.getCashBalance().setScale(0, java.math.RoundingMode.DOWN),
                wallet.getPendingBalance().setScale(0, java.math.RoundingMode.DOWN)
        );

        return PlatformWalletOverviewResponse.builder()
                .totalCustomerDeposit(wallet.getReceivedTotal())
                .pendingBalance(wallet.getPendingBalance())
                .doneBalance(wallet.getDoneBalance())
                .refundedTotal(wallet.getRefundedTotal())
                .commissionBalance(wallet.getCommissionBalance())
                .cashBalance(wallet.getCashBalance())
                .totalBalance(wallet.getTotalBalance())
                .pendingOrderCount(pendingOrderCount)
                .doneOrderCount(doneOrderCount)
                .lastUpdatedAt(wallet.getUpdatedAt())
                .summary(summary)
                .build();
    }

    @Override
    public Page<PlatformTransactionResponse> getFlatWalletTransactions(
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return transactionRepository.findFlatWalletTransactions(type, status, from, to, pageable)
                .map(this::mapToTransactionResponse);
    }

   // ------------------------------------------------------------------------------ OverView Flat Wallet ------------------------------------------------------------------------------ //


    @Override
    public FlatGhnDebtOverviewResponse getFlatGhnDebtOverview(LocalDateTime from, LocalDateTime to) {

        List<FlatDebtOrderRow> rows = storeOrderRepository.findFlatDebtRows(from, to);

        BigDecimal orderDebtToGHN = BigDecimal.ZERO;

        BigDecimal orderStoreOutstanding = BigDecimal.ZERO;
        BigDecimal orderStorePaid = BigDecimal.ZERO;

        BigDecimal customerShipPaid = BigDecimal.ZERO;

        // ✅ NEW: flat đã trả GHN / còn nợ GHN (order part) dựa vào ghnDebtFinalized
        BigDecimal orderFlatPaidToGHN = BigDecimal.ZERO;
        BigDecimal orderFlatOutstandingToGHN = BigDecimal.ZERO;

        for (FlatDebtOrderRow r : rows) {
            BigDecimal shipReal = nz(r.getShippingFeeReal());
            BigDecimal shipCus  = nz(r.getShippingFee());
            boolean delivered = r.getDeliveredAt() != null;
            boolean returningNotDelivered = (r.getDeliveredAt() == null)
                    && "RETURNING".equalsIgnoreCase(r.getStatus());
            boolean paidByShop = Boolean.TRUE.equals(r.getPaidByShop());

            // ===== 1) Nợ GHN theo rule =====
            BigDecimal debtToGhnForOrder = BigDecimal.ZERO;
            if (delivered) {
                debtToGhnForOrder = shipReal;
            } else if (returningNotDelivered) {
                debtToGhnForOrder = shipReal.multiply(new BigDecimal("1.5"));
            }

            if (debtToGhnForOrder.compareTo(BigDecimal.ZERO) <= 0) continue;

            orderDebtToGHN = orderDebtToGHN.add(debtToGhnForOrder);

            // ===== 2) Khách trả ship (chỉ delivered) =====
            if (delivered) {
                customerShipPaid = customerShipPaid.add(shipCus);
            }

            // ===== 3) Store trả / chưa trả cho flat =====
            if (paidByShop) orderStorePaid = orderStorePaid.add(debtToGhnForOrder);
            else orderStoreOutstanding = orderStoreOutstanding.add(debtToGhnForOrder);

            // ===== 4) Flat đã trả GHN hay chưa? (dựa vào ghnDebtFinalized) =====
            // NOTE: FlatDebtOrderRow hiện chưa có getGhnDebtFinalized() => bạn cần add field vào projection/query
            // boolean finalized = Boolean.TRUE.equals(r.getGhnDebtFinalized());
            // if (finalized) orderFlatPaidToGHN = orderFlatPaidToGHN.add(debtToGhnForOrder);
            // else orderFlatOutstandingToGHN = orderFlatOutstandingToGHN.add(debtToGhnForOrder);
        }

        // ===== Return shipping fee =====
        BigDecimal returnOutstanding = nz(returnShippingFeeRepository.sumUnpaidReturnByRange(from, to));
        BigDecimal returnPaid        = nz(returnShippingFeeRepository.sumPaidReturnByRange(from, to));
        BigDecimal returnTotal       = returnOutstanding.add(returnPaid);

        // ✅ NEW: flat đã trả GHN / còn nợ GHN (return part)
        // Bạn cần 2 query sumReturnFinalized / sumReturnNotFinalized (dựa vào ghnDebtFinalized)
        BigDecimal returnFlatPaidToGHN = nz(returnShippingFeeRepository.sumReturnFinalizedByRange(from, to));
        BigDecimal returnFlatOutstandingToGHN = nz(returnShippingFeeRepository.sumReturnNotFinalizedByRange(from, to));

        // ===== Tổng nợ GHN của flat =====
        BigDecimal flatDebtToGHN = orderDebtToGHN.add(returnTotal);

        // ===== Tổng store nợ flat =====
        BigDecimal storeOutstanding = orderStoreOutstanding.add(returnOutstanding);
        BigDecimal storePaid = orderStorePaid.add(returnPaid);
        BigDecimal storeTotal = storeOutstanding.add(storePaid);

        // ===== Flat paid/outstanding to GHN =====
        BigDecimal flatPaidToGHN = orderFlatPaidToGHN.add(returnFlatPaidToGHN);
        BigDecimal flatOutstandingToGHN = flatDebtToGHN.subtract(flatPaidToGHN);

        // flatNet (theo công thức bạn chốt)
        BigDecimal flatNet = customerShipPaid
                .add(storePaid)
                .add(storeOutstanding)
                .subtract(flatDebtToGHN);

        return FlatGhnDebtOverviewResponse.builder()
                .from(from)
                .to(to)

                .flatDebtToGHN(flatDebtToGHN)
                .flatPaidToGHN(flatPaidToGHN)
                .flatOutstandingToGHN(flatOutstandingToGHN)

                .storeDebtOutstandingToFlat(storeOutstanding)
                .storeDebtPaidToFlat(storePaid)
                .storeDebtTotalToFlat(storeTotal)

                .returnDebtOutstanding(returnOutstanding)
                .returnDebtPaid(returnPaid)
                .returnDebtTotal(returnTotal)

                .customerShipPaid(customerShipPaid)
                .flatNet(flatNet)

                .note("flatNet = customerShipPaid + (storePaid+returnPaid) + (storeOutstanding+returnOutstanding) - (orderDebtToGHN+returnTotal)")
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public FlatGhnOverviewResponse getFlatGhnOverview(LocalDateTime from, LocalDateTime to) {

        // ✅ chuẩn hoá range [from, toExclusive)
        LocalDateTime toExclusive = (to == null) ? null : to.plusDays(1);

        FlatOrderAgg2 agg = storeOrderRepository.aggFlatOverview(from, toExclusive);

        BigDecimal flatDebtShipToGHN = nz(agg.getFlatDebtShipToGhn());
        BigDecimal customerShipPaid  = nz(agg.getCustomerShipPaid());
        BigDecimal storePaidToFlat   = nz(agg.getStoreDebtPaidToFlat());

        BigDecimal storeOutstandingToFlat = nz(storeWalletRepository.sumAllStoreDebtBalance());

        return FlatGhnOverviewResponse.builder()
                .from(from)
                .toExclusive(toExclusive)
                .flatDebtShipToGHN(flatDebtShipToGHN)
                .customerShipPaid(customerShipPaid)
                .storeDebtPaidToFlat(storePaidToFlat)
                .storeDebtOutstandingToFlat(storeOutstandingToFlat)
                .storeDebtTotalToFlat(storePaidToFlat.add(storeOutstandingToFlat))
                .note("Exclude statuses: UNPAID, CONFIRMED, AWAITING_SHIPMENT, EXCEPTION, CANCELLED. Debt rule: delivered=shipReal, returning(not delivered)=shipReal*1.5. Range=[from,toExclusive). storePaidToFlat uses paid_by_shop=true with same debt rule.")
                .build();
    }

    // =========================================================
    // 1️⃣ OVERVIEW – DOANH THU NỀN TẢNG
    // =========================================================
    @Override
    public PlatformRevenueOverviewResponse getPlatformRevenueOverview() {

        var agg = storeOrderItemRepository.aggregateDeliveredEligibleItems();

        return PlatformRevenueOverviewResponse.builder()
                .deliveredItemCount(agg.getDeliveredItemCount() == null ? 0L : agg.getDeliveredItemCount())
                .totalItemRevenue(agg.getTotalItemRevenue() == null ? BigDecimal.ZERO : agg.getTotalItemRevenue())
                .platformFeeRevenue(agg.getPlatformFeeRevenue() == null ? BigDecimal.ZERO : agg.getPlatformFeeRevenue())
                .build();
    }

    // =========================================================
    // 2️⃣ CHART THEO THÁNG
    // =========================================================
    @Override
    public List<PlatformGrowthChartPoint> getPlatformGrowthChartByMonth() {

        // --- Line 1: doanh thu nền tảng theo tháng ---
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] r : storeOrderItemRepository.revenueByMonth()) {
            int year = (int) r[0];
            int month = (int) r[1];
            BigDecimal revenue = (BigDecimal) r[2];
            revenueMap.put(year + "-" + month, revenue);
        }

        // --- Line 2: số return theo tháng ---
        Map<String, Long> returnCountMap = new HashMap<>();
        for (Object[] r : returnRequestRepository.returnCountByMonth()) {
            int year = (int) r[0];
            int month = (int) r[1];
            Long cnt = (Long) r[2];
            returnCountMap.put(year + "-" + month, cnt);
        }

        List<PlatformGrowthChartPoint> result = new ArrayList<>();

        for (String key : revenueMap.keySet()) {
            String[] parts = key.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            BigDecimal revenue = revenueMap.getOrDefault(key, BigDecimal.ZERO);
            long returnCount = returnCountMap.getOrDefault(key, 0L);

            long deliveredCount =
                    storeOrderItemRepository.countDeliveredItems(year, month);

            BigDecimal returnRate = deliveredCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(returnCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(deliveredCount), 2, RoundingMode.HALF_UP);

            result.add(
                    PlatformGrowthChartPoint.builder()
                            .year(year)
                            .month(month)
                            .platformRevenue(revenue)
                            .returnRate(returnRate)
                            .build()
            );
        }

        result.sort(Comparator
                .comparing(PlatformGrowthChartPoint::getYear)
                .thenComparing(PlatformGrowthChartPoint::getMonth));

        return result;
    }

    // =========================================================
    // 3️⃣ CHART THEO NĂM
    // =========================================================
    @Override
    public List<PlatformGrowthChartPoint> getPlatformGrowthChartByYear() {

        Map<Integer, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] r : storeOrderItemRepository.revenueByYear()) {
            int year = (int) r[0];
            BigDecimal revenue = (BigDecimal) r[1];
            revenueMap.put(year, revenue);
        }

        Map<Integer, Long> returnMap = new HashMap<>();
        for (Object[] r : returnRequestRepository.returnCountByYear()) {
            int year = (int) r[0];
            Long cnt = (Long) r[1];
            returnMap.put(year, cnt);
        }

        List<PlatformGrowthChartPoint> result = new ArrayList<>();

        for (Integer year : revenueMap.keySet()) {
            BigDecimal revenue = revenueMap.getOrDefault(year, BigDecimal.ZERO);
            long returns = returnMap.getOrDefault(year, 0L);

            long delivered =
                    storeOrderItemRepository.countDeliveredItemsByYear(year);

            BigDecimal returnRate = delivered == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(returns)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(delivered), 2, RoundingMode.HALF_UP);

            result.add(
                    PlatformGrowthChartPoint.builder()
                            .year(year)
                            .month(0)
                            .platformRevenue(revenue)
                            .returnRate(returnRate)
                            .build()
            );
        }

        result.sort(Comparator.comparing(PlatformGrowthChartPoint::getYear));
        return result;
    }

}
