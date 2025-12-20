package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreDashboardResponses;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.ReturnRequestRepository;
import org.example.audio_ecommerce.repository.StoreOrderItemRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.StoreDashboardService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreDashboardServiceImpl implements StoreDashboardService {

    private final SecurityUtils securityUtils;
    private final StoreOrderItemRepository storeOrderItemRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    // =========================
    // Helpers (SAFE)
    // =========================
    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("❌ from/to is required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("❌ from must be <= to");
        }
    }

    private BigDecimal bd(Object v) {
        if (v == null) return BigDecimal.ZERO;

        try {
            if (v instanceof BigDecimal b) return b;

            if (v instanceof java.math.BigInteger bi) return new BigDecimal(bi);

            if (v instanceof Number n) {
                // dùng toString để tránh lỗi scientific/format bất thường
                return new BigDecimal(n.toString());
            }

            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return BigDecimal.ZERO;

            // bỏ dấu phẩy nếu có format 1,234.56
            s = s.replace(",", "");

            return new BigDecimal(s);
        } catch (Exception ex) {
            // ✅ không cho chết 500; log ra để debug
            System.err.println("❌ bd() parse failed: value=[" + v + "], type=" + v.getClass().getName());
            ex.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    private long lng(Object v) {
        if (v == null) return 0L;

        try {
            if (v instanceof Number n) return n.longValue();

            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0L;

            s = s.replace(",", "");
            return Long.parseLong(s);
        } catch (Exception ex) {
            System.err.println("❌ lng() parse failed: value=[" + v + "], type=" + v.getClass().getName());
            ex.printStackTrace();
            return 0L;
        }
    }

    /**
     * Convert DB value to UUID safely:
     * - UUID -> UUID
     * - byte[16] (BINARY(16)) -> UUID
     * - String -> UUID.fromString
     */
    private UUID toUuid(Object v) {
        if (v == null) return null;

        try {
            if (v instanceof UUID u) return u;

            if (v instanceof byte[] bytes) {
                if (bytes.length != 16) {
                    throw new IllegalArgumentException("UUID bytes length must be 16, got " + bytes.length);
                }
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                long high = bb.getLong();
                long low = bb.getLong();
                return new UUID(high, low);
            }

            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;

            return UUID.fromString(s);
        } catch (Exception ex) {
            System.err.println("❌ toUuid() failed: value=[" + v + "], type=" + v.getClass().getName());
            ex.printStackTrace();
            return null;
        }
    }

    private Set<ReturnStatus> excludedReturnStatuses() {
        return EnumSet.of(
                ReturnStatus.PENDING,
                ReturnStatus.CANCELLED,
                ReturnStatus.DISPUTE_RESOLVED_SHOP,
                ReturnStatus.REJECTED
        );
    }

    // =========================
    // 1) SUMMARY
    // =========================
    @Override
    public StoreDashboardResponses.StoreDashboardSummaryResponse getSummary(LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);

        UUID storeId = securityUtils.getCurrentStoreId();

        // ✅ Lấy tất cả items delivered + (eligible OR payout)
        List<StoreOrderItem> items = storeOrderItemRepository
                .findDeliveredEligibleOrPayoutItems(storeId, from, to);

        if (items == null) items = Collections.emptyList();

        // ✅ SUM trong Java
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal platformFeePaid = BigDecimal.ZERO;
        long itemsSold = 0L;

        for (StoreOrderItem i : items) {
            // (final_line_total ưu tiên) nếu null thì lấy lineTotal
            BigDecimal lineAmount = i.getFinalLineTotal();
            if (lineAmount == null) lineAmount = i.getLineTotal();
            if (lineAmount == null) lineAmount = BigDecimal.ZERO;

            grossRevenue = grossRevenue.add(lineAmount);

            BigDecimal fee = i.getPlatformFeeAmount();
            if (fee == null) fee = BigDecimal.ZERO;

            platformFeePaid = platformFeePaid.add(fee);

            // quantity int -> long
            itemsSold += (long) i.getQuantity();
        }

        // ✅ debug cho dễ soi
        System.out.println("DEBUG items size=" + items.size());
        System.out.println("DEBUG grossRevenue=" + grossRevenue + ", fee=" + platformFeePaid + ", itemsSold=" + itemsSold);

        long deliveredOrderCount = storeOrderRepository.countDeliveredOrders(storeId, from, to);

        // ✅ TOP 10 (giữ nguyên)
        List<Object[]> top10Rows = storeOrderItemRepository.findTop10SellingRefIdDelivered(storeId);

        List<StoreDashboardResponses.StoreTopSellingResponse> top10Selling =
                (top10Rows == null ? List.<Object[]>of() : top10Rows)
                        .stream()
                        .map(r -> StoreDashboardResponses.StoreTopSellingResponse.builder()
                                .refId(toUuid(r[0]))
                                .quantitySold(lng(r[1]))
                                .build())
                        .collect(Collectors.toList());

        return StoreDashboardResponses.StoreDashboardSummaryResponse.builder()
                .grossRevenue(grossRevenue)
                .platformFeePaid(platformFeePaid)
                .netRevenue(grossRevenue.subtract(platformFeePaid))
                .deliveredOrderCount(deliveredOrderCount)
                .itemsSold(itemsSold)
                .top10Selling(top10Selling)
                .build();
    }

    // =========================
    // 2) RETURN STATS
    // =========================
    @Override
    public StoreDashboardResponses.StoreReturnStatsResponse getReturnStats(LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);

        UUID storeId = securityUtils.getCurrentStoreId();
        Set<ReturnStatus> excludedStatuses = excludedReturnStatuses();

        long returnCount = returnRequestRepository.countValidReturns(storeId, excludedStatuses, from, to);

        List<Object[]> topReturned = returnRequestRepository.topReturnedProducts(storeId, excludedStatuses, from, to);

        List<StoreDashboardResponses.ReturnedProductCount> top5 = (topReturned == null ? List.<Object[]>of() : topReturned)
                .stream()
                .limit(5)
                .map(r -> StoreDashboardResponses.ReturnedProductCount.builder()
                        .productId(toUuid(r[0]))
                        .count(lng(r[1]))
                        .build())
                .collect(Collectors.toList());

        return StoreDashboardResponses.StoreReturnStatsResponse.builder()
                .returnCount(returnCount)
                .top5ReturnedProducts(top5)
                .build();
    }

    // =========================
    // 3) GROWTH BY MONTH
    // =========================
    @Override
    public StoreDashboardResponses.GrowthResponse growthByMonth(int year) {
        UUID storeId = securityUtils.getCurrentStoreId();

        List<Object[]> itemGrowth = storeOrderItemRepository.growthByMonth(storeId, year);
        List<Object[]> orderGrowth = storeOrderRepository.deliveredOrdersByMonth(storeId, year);

        Map<Integer, Long> deliveredOrdersByMonth = new HashMap<>();
        if (orderGrowth != null) {
            for (Object[] r : orderGrowth) {
                // y, m, deliveredOrders
                int month = ((Number) r[1]).intValue();
                long count = lng(r[2]);
                deliveredOrdersByMonth.put(month, count);
            }
        }

        List<StoreDashboardResponses.GrowthPointResponse> points = new ArrayList<>();
        if (itemGrowth != null) {
            for (Object[] r : itemGrowth) {
                // y, m, gross, fee, itemsSold
                int y = ((Number) r[0]).intValue();
                int m = ((Number) r[1]).intValue();
                BigDecimal gross = bd(r[2]);
                BigDecimal fee = bd(r[3]);
                long itemsSold = lng(r[4]);

                long deliveredOrders = deliveredOrdersByMonth.getOrDefault(m, 0L);

                points.add(StoreDashboardResponses.GrowthPointResponse.builder()
                        .year(y)
                        .month(m)
                        .grossRevenue(gross)
                        .platformFeePaid(fee)
                        .netRevenue(gross.subtract(fee))
                        .deliveredOrderCount(deliveredOrders)
                        .itemsSold(itemsSold)
                        .build());
            }
        }

        return StoreDashboardResponses.GrowthResponse.builder()
                .year(year)
                .granularity("MONTH")
                .points(points)
                .build();
    }

    // =========================
    // 4) GROWTH BY YEAR
    // =========================
    @Override
    public StoreDashboardResponses.GrowthResponse growthByYear(int fromYear, int toYear) {
        if (fromYear > toYear) {
            throw new IllegalArgumentException("❌ fromYear must be <= toYear");
        }

        UUID storeId = securityUtils.getCurrentStoreId();

        List<Object[]> itemGrowth = storeOrderItemRepository.growthByYear(storeId, fromYear, toYear);
        List<Object[]> orderGrowth = storeOrderRepository.deliveredOrdersByYear(storeId, fromYear, toYear);

        Map<Integer, Long> deliveredOrdersByYear = new HashMap<>();
        if (orderGrowth != null) {
            for (Object[] r : orderGrowth) {
                // y, deliveredOrders
                int y = ((Number) r[0]).intValue();
                long count = lng(r[1]);
                deliveredOrdersByYear.put(y, count);
            }
        }

        List<StoreDashboardResponses.GrowthPointResponse> points = new ArrayList<>();
        if (itemGrowth != null) {
            for (Object[] r : itemGrowth) {
                // y, gross, fee, itemsSold
                int y = ((Number) r[0]).intValue();
                BigDecimal gross = bd(r[1]);
                BigDecimal fee = bd(r[2]);
                long itemsSold = lng(r[3]);

                long deliveredOrders = deliveredOrdersByYear.getOrDefault(y, 0L);

                points.add(StoreDashboardResponses.GrowthPointResponse.builder()
                        .year(y)
                        .month(null)
                        .grossRevenue(gross)
                        .platformFeePaid(fee)
                        .netRevenue(gross.subtract(fee))
                        .deliveredOrderCount(deliveredOrders)
                        .itemsSold(itemsSold)
                        .build());
            }
        }

        return StoreDashboardResponses.GrowthResponse.builder()
                .year(fromYear)
                .granularity("YEAR")
                .points(points)
                .build();
    }

    // =========================
    // 5) FULL DASHBOARD
    // =========================
    @Override
    public StoreDashboardResponses.StoreDashboardFullResponse getFull(LocalDateTime from, LocalDateTime to, Integer year) {
        validateRange(from, to);

        StoreDashboardResponses.StoreDashboardSummaryResponse summary = getSummary(from, to);
        StoreDashboardResponses.StoreReturnStatsResponse returns = getReturnStats(from, to);
        StoreDashboardResponses.GrowthResponse growth = (year != null) ? growthByMonth(year) : null;

        return StoreDashboardResponses.StoreDashboardFullResponse.builder()
                .summary(summary)
                .returns(returns)
                .growth(growth)
                .build();
    }
}
