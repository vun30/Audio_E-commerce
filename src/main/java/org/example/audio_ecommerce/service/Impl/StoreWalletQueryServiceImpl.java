// package org.example.audio_ecommerce.service.Impl;
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreWalletItemResponse;
import org.example.audio_ecommerce.dto.response.StoreWalletSummaryFinalResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.StoreWalletBucket;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.StoreOrderItemRepository;
import org.example.audio_ecommerce.service.StoreWalletQueryService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreWalletQueryServiceImpl implements StoreWalletQueryService {

    private final StoreOrderItemRepository itemRepo;

    @Override
    @Transactional(readOnly = true)
    public StoreWalletSummaryFinalResponse getSummary(UUID storeId) {
        List<StoreOrderItem> items =
                itemRepo.findAllByStoreOrder_Store_StoreId(storeId);

        BigDecimal estimated = BigDecimal.ZERO;
        BigDecimal pending   = BigDecimal.ZERO;
        BigDecimal done      = BigDecimal.ZERO;
        BigDecimal net       = BigDecimal.ZERO;

        for (StoreOrderItem it : items) {
            StoreOrder so = it.getStoreOrder();
            if (so == null) continue;

            // ❌ Bỏ qua order CANCELLED / UNPAID
            OrderStatus st = so.getStatus();
            if (st == OrderStatus.CANCELLED || st == OrderStatus.UNPAID) {
                continue;
            }

            // ❌ Bỏ qua item đã return hoàn toàn
            if (Boolean.TRUE.equals(it.getIsReturned())) {
                continue;
            }

            // Các cờ trạng thái cho dễ đọc
            boolean isPayout   = Boolean.TRUE.equals(it.getIsPayout());
            boolean eligible   = Boolean.TRUE.equals(it.getEligibleForPayout());
            // boolean returned = Boolean.TRUE.equals(it.getIsReturned()); // đã lọc ở trên

            BigDecimal gross   = resolveGross(it);
            BigDecimal netItem = resolveNet(it);

            // =====================================================
            // 1) ƯỚC TÍNH
            //    = tổng tiền hàng của item chưa payout
            //    (is_payout = false)
            // =====================================================
            if (!isPayout) {
                estimated = estimated.add(gross);
            }

            // =====================================================
            // 2) PENDING
            //    = subset của ước tính:
            //      những item chưa payout và chưa đủ điều kiện payout
            //      (is_payout = false AND eligible_for_payout = false)
            // =====================================================
            if (!isPayout && !eligible) {
                pending = pending.add(gross);
            }

            // =====================================================
            // 3) DONE (GROSS)
            //    = tổng tiền hàng của item đã payout
            //      (is_payout = true, optional: eligible_for_payout = true)
            // =====================================================
            if (!isPayout && eligible) {
                done = done.add(gross);

                // =================================================
                // 4) LÃI RÒNG
                //    = tổng tiền lợi nhuận của mọi item đã payout:
                //      netItem = gross – phí nền tảng – ship chênh – giá vốn
                // =================================================
                net = net.add(netItem);
            }
        }

        return StoreWalletSummaryFinalResponse.builder()
                .storeId(storeId)
                .estimatedGross(estimated)
                .pendingGross(pending)
                .doneGross(done)
                .netProfit(net)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<StoreWalletItemResponse> getItemsByBucket(
            UUID storeId,
            StoreWalletBucket bucket,
            int page,
            int size
    ) {
        PageRequest pr = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "storeOrder.createdAt").descending());

        Page<StoreOrderItem> pageItems;

        switch (bucket) {
            case ESTIMATED ->
                    pageItems = itemRepo.findEstimatedItems(storeId, pr);
            case PENDING ->
                    pageItems = itemRepo.findPendingItems(storeId, pr);
            case DONE ->
                    pageItems = itemRepo.findDoneItems(storeId, pr);
            default -> throw new IllegalArgumentException("Unsupported bucket: " + bucket);
        }

        List<StoreWalletItemResponse> list = pageItems.getContent()
                .stream()
                .map(this::toWalletItemDto)
                .toList();

        return PagedResult.<StoreWalletItemResponse>builder()
                .items(list)
                .totalElements(pageItems.getTotalElements())
                .totalPages(pageItems.getTotalPages())
                .page(pageItems.getNumber())
                .size(pageItems.getSize())
                .build();
    }

    // ================== Helpers ==================

    private BigDecimal resolveGross(StoreOrderItem it) {
        BigDecimal gross = nz(it.getFinalLineTotal());
        if (gross.compareTo(BigDecimal.ZERO) == 0) {
            gross = nz(it.getAmountCharged());
        }
        if (gross.compareTo(BigDecimal.ZERO) == 0) {
            gross = nz(it.getLineTotal());
        }
        return gross;
    }

    private BigDecimal resolveNet(StoreOrderItem it) {
        BigDecimal gross = resolveGross(it);

        BigDecimal platformFee = nz(it.getPlatformFeeAmount());
        if (platformFee.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal pct = nz(it.getPlatformFeePercentage()); // ví dụ 5.00
            if (pct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = pct
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                platformFee = gross.multiply(rate)
                        .setScale(0, RoundingMode.DOWN);
            }
        }

        BigDecimal shipExtra = nz(it.getShippingExtraForStore());

        BigDecimal unitCost = nz(it.getCostPrice());
        int qty = it.getQuantity() > 0 ? it.getQuantity() : 1;
        BigDecimal cost = unitCost.multiply(BigDecimal.valueOf(qty));

        BigDecimal net = gross
                .subtract(platformFee)
                .subtract(shipExtra)
                .subtract(cost);

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            net = BigDecimal.ZERO;
        }
        return net;
    }

    private StoreWalletItemResponse toWalletItemDto(StoreOrderItem it) {
        StoreOrder so = it.getStoreOrder();

        BigDecimal gross = resolveGross(it);
        BigDecimal netItem = resolveNet(it);
        BigDecimal platformFee = nz(it.getPlatformFeeAmount());
        if (platformFee.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal pct = nz(it.getPlatformFeePercentage());
            if (pct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = pct
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                platformFee = gross.multiply(rate).setScale(0, RoundingMode.DOWN);
            }
        }

        BigDecimal shipExtra = nz(it.getShippingExtraForStore());

        BigDecimal costOfGoods = nz(it.getCostPrice())
                .multiply(BigDecimal.valueOf(
                        it.getQuantity() > 0 ? it.getQuantity() : 1));

        return StoreWalletItemResponse.builder()
                .storeOrderItemId(it.getId())
                .storeOrderId(so != null ? so.getId() : null)
                .orderCode(so != null ? so.getOrderCode() : null)
                .orderCreatedAt(so != null ? so.getCreatedAt() : null)
                .productName(it.getName())
                .variantOptionName(it.getVariantOptionName())
                .variantOptionValue(it.getVariantOptionValue())
                .quantity(it.getQuantity())
                .grossAmount(gross)
                .platformFee(platformFee)
                .shippingExtra(shipExtra)
                .costOfGoods(costOfGoods)
                .netProfit(netItem)
                .eligibleForPayout(it.getEligibleForPayout())
                .isPayout(it.getIsPayout())
                .isReturned(it.getIsReturned())
                .orderStatus(so != null && so.getStatus() != null
                        ? so.getStatus().name()
                        : null)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
