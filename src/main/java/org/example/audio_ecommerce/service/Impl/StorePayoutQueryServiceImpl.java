package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StorePayoutItemResponse;
import org.example.audio_ecommerce.dto.response.StorePayoutSummaryResponse;
import org.example.audio_ecommerce.entity.Enum.StorePayoutBucket;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.StoreOrderItemRepository;
import org.example.audio_ecommerce.service.StorePayoutQueryService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorePayoutQueryServiceImpl implements StorePayoutQueryService {

    private final StoreOrderItemRepository storeOrderItemRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public StorePayoutSummaryResponse getSummary(LocalDateTime from, LocalDateTime to) {

        UUID storeId = securityUtils.getCurrentStoreId();

        LocalDateTime f = (from == null) ? LocalDateTime.of(2000, 1, 1, 0, 0) : from;
        LocalDateTime t = (to == null) ? LocalDateTime.now() : to;

        // ✅ repo giờ trả List<Object[]> (aggregate query)
        Object[] pendingRow = firstRow(storeOrderItemRepository.sumPendingBalance(storeId, f, t));
        long pendingCount = toLong(pendingRow, 0);
        BigDecimal pendingGross = toBd(pendingRow, 1);

        Object[] feePayableRow = firstRow(storeOrderItemRepository.sumPlatformFeePayable(storeId, f, t));
        long eligibleNotPayoutCount = toLong(feePayableRow, 0);
        BigDecimal eligibleNotPayoutGross = toBd(feePayableRow, 1);
        BigDecimal platformFeePayable = toBd(feePayableRow, 2);

        Object[] availableRow = firstRow(storeOrderItemRepository.sumAvailableBalance(storeId, f, t));
        long payoutDoneCount = toLong(availableRow, 0);
        BigDecimal availableGross = toBd(availableRow, 1);
        BigDecimal platformFeePaid = toBd(availableRow, 2);
        BigDecimal availableNet = availableGross.subtract(platformFeePaid);

        return StorePayoutSummaryResponse.builder()
                .pendingCount(pendingCount)
                .pendingGross(pendingGross)

                .eligibleNotPayoutCount(eligibleNotPayoutCount)
                .eligibleNotPayoutGross(eligibleNotPayoutGross)
                .platformFeePayable(platformFeePayable)

                .payoutDoneCount(payoutDoneCount)
                .availableGross(availableGross)
                .platformFeePaid(platformFeePaid)
                .availableNet(availableNet)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<StorePayoutItemResponse> getBreakdownItems(
            StorePayoutBucket bucket,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        UUID storeId = securityUtils.getCurrentStoreId();

        LocalDateTime f = (from == null) ? LocalDateTime.of(2000, 1, 1, 0, 0) : from;
        LocalDateTime t = (to == null) ? LocalDateTime.now() : to;

        Pageable pageable = PageRequest.of(page, size, Sort.by("deliveredAt").descending());

        Page<StoreOrderItem> p;
        switch (bucket) {
            case PENDING -> p = storeOrderItemRepository.findPendingBreakdown(storeId, f, t, pageable);
            case ELIGIBLE_NOT_PAYOUT -> p = storeOrderItemRepository.findEligibleNotPayoutBreakdown(storeId, f, t, pageable);
            case PAYOUT_DONE -> p = storeOrderItemRepository.findPayoutDoneBreakdown(storeId, f, t, pageable);
            default -> throw new IllegalArgumentException("Unsupported bucket: " + bucket);
        }

        List<StorePayoutItemResponse> items = p.getContent().stream()
                .map(this::toPayoutItemDto)
                .collect(Collectors.toList());

        return PagedResult.<StorePayoutItemResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
    }

    private StorePayoutItemResponse toPayoutItemDto(StoreOrderItem i) {
        BigDecimal gross = nz(i.getFinalLineTotal());
        BigDecimal pct = nz(i.getPlatformFeePercentage());

        BigDecimal fee = gross
                .multiply(pct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal net = gross.subtract(fee);

        return StorePayoutItemResponse.builder()
                .itemId(i.getId())
                .storeOrderId(i.getStoreOrder() != null ? i.getStoreOrder().getId() : null)
                .orderCode(i.getStoreOrder() != null ? i.getStoreOrder().getOrderCode() : null)
                .finalLineTotal(gross)
                .platformFeePercentage(pct)
                .platformFeeAmount(fee)
                .netAfterFee(net)
                .eligibleForPayout(i.getEligibleForPayout())
                .isPayout(i.getIsPayout())
                .isReturned(i.getIsReturned())
                .deliveredAt(i.getDeliveredAt())
                .build();
    }

    // =========================
    // Helpers (FIX cast issues)
    // =========================

    private Object[] firstRow(List<Object[]> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return rows.get(0);
    }

    private long toLong(Object[] row, int idx) {
        if (row == null || row.length <= idx || row[idx] == null) return 0L;
        if (row[idx] instanceof Number n) return n.longValue();
        return Long.parseLong(row[idx].toString());
    }

    private BigDecimal toBd(Object[] row, int idx) {
        if (row == null || row.length <= idx || row[idx] == null) return BigDecimal.ZERO;
        Object v = row[idx];
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
