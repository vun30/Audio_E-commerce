package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.repository.StoreStatsRepository;
import org.example.audio_ecommerce.service.ShopStatsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopStatsServiceImpl implements ShopStatsService {

    private final StoreStatsRepository repo;

    // ============================================================
    // 1) LIFETIME
    // ============================================================
    @Override
    public LifetimeStatsResponse getLifetimeStats(UUID storeId) {

        List<StoreOrder> deliveredOrders = repo.findAllDeliveredOrders(storeId);
        List<ReturnRequest> returns = repo.findAllReturnRequests(storeId);

        // Shipping Difference Fee (tất cả đơn đã trừ phí ship)
        BigDecimal diffFee = repo.sumShippingDifferenceBetween(
                storeId,
                LocalDate.of(1970, 1, 1).atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        // Return Shipping Fee Lifetime
        BigDecimal returnShipFee = repo.sumReturnShippingFeeBetween(
                storeId,
                LocalDate.of(1970, 1, 1).atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        return buildStats(storeId, deliveredOrders, returns, diffFee, returnShipFee);
    }

    // ============================================================
    // 2) RANGE
    // ============================================================
    @Override
    public LifetimeStatsResponse getLifetimeStatsByRange(UUID storeId, LocalDate from, LocalDate to) {

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<StoreOrder> deliveredOrders = repo.findDeliveredOrdersBetween(storeId, start, end);
        List<ReturnRequest> returns = repo.findReturnRequestsBetween(storeId, start, end);

        BigDecimal diffFee = repo.sumShippingDifferenceBetween(storeId, start, end);
        BigDecimal returnShipFee = repo.sumReturnShippingFeeBetween(storeId, start, end);

        return buildStats(storeId, deliveredOrders, returns, diffFee, returnShipFee);
    }

    // ============================================================
    // 3) YEAR GROWTH (12 MONTHS)
    // ============================================================
    @Override
    public List<MonthlyGrowthPoint> getYearGrowth(UUID storeId, int year) {

        List<MonthlyGrowthPoint> result = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {

            LocalDateTime start = LocalDate.of(year, m, 1).atStartOfDay();
            LocalDateTime end = (m == 12)
                    ? LocalDate.of(year + 1, 1, 1).atStartOfDay()
                    : LocalDate.of(year, m + 1, 1).atStartOfDay();

            // Delivered
            List<StoreOrder> delivered = repo.findDeliveredOrdersBetween(storeId, start, end);
            long deliveredCount = delivered.size();

            BigDecimal revenue = delivered.stream()
                    .map(StoreOrder::getGrandTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Return Success
            long returnSuccess = repo.countReturnSuccessBetween(storeId, start, end);

            double returnRate = deliveredCount == 0
                    ? 0
                    : (returnSuccess * 100.0) / deliveredCount;

            // Shipping Difference
            BigDecimal diffFee = repo.sumShippingDifferenceBetween(storeId, start, end);

            // Return Shipping Fee
            BigDecimal returnShipFee = repo.sumReturnShippingFeeBetween(storeId, start, end);

            // Total actual shipping collected
            BigDecimal shippingCollected = repo.sumActualShippingFeeBetween(storeId, start, end);

            result.add(MonthlyGrowthPoint.builder()
                    .month(m)
                    .orders(deliveredCount)
                    .revenue(revenue)
                    .returnSuccess(returnSuccess)
                    .returnRate(round2(returnRate))
                    .shippingDifference(diffFee)
                    .returnShippingFee(returnShipFee)
                    .shippingCollected(shippingCollected)
                    .build());
        }

        return result;
    }

    // ============================================================
    // CORE BUILD
    // ============================================================
    private LifetimeStatsResponse buildStats(
            UUID storeId,
            List<StoreOrder> deliveredOrders,
            List<ReturnRequest> returns,
            BigDecimal diffShip,
            BigDecimal returnShipFee
    ) {

        long totalDelivered = deliveredOrders.size();

        List<UUID> orderIds = deliveredOrders.stream()
                .map(StoreOrder::getId)
                .toList();

        List<StoreOrderItem> items = repo.findItemsByOrderIds(orderIds);

        // Revenue
        BigDecimal totalRevenue = sum(items, StoreOrderItem::getFinalLineTotal);

        BigDecimal totalPlatformFee = items.stream()
                .map(i -> calcPlatformFee(i.getFinalLineTotal(), i.getPlatformFeePercentage()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = totalRevenue.subtract(totalPlatformFee);

        // Return
        long returnSuccess = returns.stream()
                .filter(this::isReturnCompleted)
                .count();

        double returnRate = totalDelivered == 0
                ? 0
                : returnSuccess * 100.0 / totalDelivered;

        // Top 10 Sell
        List<TopProductLifetime> top10 = computeTopProducts(items);

        // Top 1 Return
        TopReturnProduct topReturn = computeTopReturnProduct(returns);

        return LifetimeStatsResponse.builder()
                .totalDeliveredOrders(totalDelivered)
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .totalNetRevenue(netRevenue)
                .totalReturnRequests(returnSuccess)
                .returnRate(round2(returnRate))
                .top10Products(top10)
                .topReturnProduct(topReturn)
                .totalShippingDifferenceFee(diffShip)
                .totalReturnShippingFee(returnShipFee)
                .build();
    }

    // ============================================================
    // HELPER
    // ============================================================
    private BigDecimal sum(List<StoreOrderItem> list,
                           java.util.function.Function<StoreOrderItem, BigDecimal> getter) {

        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcPlatformFee(BigDecimal total, BigDecimal percent) {
        if (total == null || percent == null) return BigDecimal.ZERO;

        return total.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private boolean isReturnCompleted(ReturnRequest r) {
        return r.getStatus() == ReturnStatus.RETURN_DONE ||
                r.getStatus() == ReturnStatus.DISPUTE_RESOLVED_CUSTOMER ||
                r.getStatus() == ReturnStatus.REFUNDED;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ==============================
    // TOP 10 PRODUCTS
    // ==============================
    private List<TopProductLifetime> computeTopProducts(List<StoreOrderItem> items) {

        Map<UUID, List<StoreOrderItem>> grouped =
                items.stream().collect(Collectors.groupingBy(StoreOrderItem::getRefId));

        return grouped.entrySet().stream()
                .map(e -> {
                    UUID pid = e.getKey();
                    List<StoreOrderItem> list = e.getValue();

                    long qty = list.stream().mapToLong(StoreOrderItem::getQuantity).sum();
                    BigDecimal rev = sum(list, StoreOrderItem::getFinalLineTotal);

                    return TopProductLifetime.builder()
                            .productId(pid)
                            .name(Optional.ofNullable(list.get(0).getName()).orElse("Unknown"))
                            .totalSoldQuantity(qty)
                            .totalRevenue(rev)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getTotalSoldQuantity(), a.getTotalSoldQuantity()))
                .limit(10)
                .toList();
    }

    // ==============================
    // TOP RETURN PRODUCT
    // ==============================
    private TopReturnProduct computeTopReturnProduct(List<ReturnRequest> returns) {

        Map<UUID, Long> grouped = returns.stream()
                .filter(this::isReturnCompleted)
                .collect(Collectors.groupingBy(ReturnRequest::getProductId, Collectors.counting()));

        if (grouped.isEmpty()) return null;

        Map.Entry<UUID, Long> max =
                grouped.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);

        if (max == null) return null;

        ReturnRequest sample =
                returns.stream()
                        .filter(r -> r.getProductId().equals(max.getKey()))
                        .findFirst()
                        .orElse(null);

        return TopReturnProduct.builder()
                .productId(max.getKey())
                .productName(sample != null ? sample.getProductName() : "Unknown")
                .returnCount(max.getValue())
                .build();
    }
}
