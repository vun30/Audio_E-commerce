package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.OrderStatusCountsResponse;
import org.example.audio_ecommerce.dto.response.OrdersByDayPoint;
import org.example.audio_ecommerce.dto.response.OrdersByDayResponse;
import org.example.audio_ecommerce.dto.response.OrderStatusRatioResponse;
import org.example.audio_ecommerce.dto.response.ShopOverviewStatsResponse;
import org.example.audio_ecommerce.dto.response.ProfitStatsResponse;
import org.example.audio_ecommerce.dto.response.TopSellingProductResponse;
import org.example.audio_ecommerce.dto.response.ProductViewAnalyticsResponse;
import org.example.audio_ecommerce.dto.response.OutOfStockProductResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.repository.StoreStatsRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.service.ShopStatsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopStatsServiceImpl implements ShopStatsService {

    private final StoreStatsRepository storeStatsRepository;
    private final ProductRepository productRepository;

    @Override
    public ShopOverviewStatsResponse getOverview(UUID storeId, LocalDate date, LocalDate startDate, LocalDate endDate) {
        // Default date = today
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        // Month range
        LocalDate monthStart = LocalDate.of(targetDate.getYear(), targetDate.getMonth(), 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        long totalOrdersToday = storeStatsRepository.countOrdersToday(storeId, startOfDay, endOfDay);
        BigDecimal revenueToday = storeStatsRepository.sumRevenueToday(storeId, startOfDay, endOfDay);
        long totalOrdersSuccess = storeStatsRepository.countByStatus(storeId, OrderStatus.COMPLETED);
        long totalOrdersCancelled = storeStatsRepository.countByStatus(storeId, OrderStatus.CANCELLED);

        BigDecimal revenueThisMonth = storeStatsRepository.sumRevenueThisMonth(storeId, monthStart, monthEnd);
        BigDecimal profitThisMonth = storeStatsRepository.sumProfitThisMonth(storeId, monthStart, monthEnd);

        long totalProductViews = storeStatsRepository.sumProductViews(storeId);
        long inStock = storeStatsRepository.countProductsInStock(storeId);
        long outOfStock = storeStatsRepository.countProductsOutOfStock(storeId);

        double conversionRate = totalProductViews > 0 ? (double) totalOrdersSuccess / (double) totalProductViews : 0.0;

        return ShopOverviewStatsResponse.builder()
                .totalOrdersToday(totalOrdersToday)
                .revenueToday(zeroIfNull(revenueToday))
                .totalOrdersSuccess(totalOrdersSuccess)
                .totalOrdersCancelled(totalOrdersCancelled)
                .revenueThisMonth(zeroIfNull(revenueThisMonth))
                .profitThisMonth(zeroIfNull(profitThisMonth))
                .conversionRate(round(conversionRate, 4))
                .totalProductViews(totalProductViews)
                .totalProductsInStock(inStock)
                .totalProductsOutOfStock(outOfStock)
                .build();
    }

    @Override
    public OrderStatusCountsResponse getOrderStatusCounts(UUID storeId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(6); // default 7 ngày gần nhất
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();

        var raw = storeStatsRepository.countGroupByStatus(storeId, start, end);
        long pending=0, processing=0, shipping=0, completed=0, cancelled=0, returned=0;
        for (Object[] row : raw) {
            var status = (OrderStatus) row[0];
            long cnt = (Long) row[1];
            switch (status) {
                case PENDING, UNPAID -> pending += cnt;
                case CONFIRMED, AWAITING_SHIPMENT, READY_FOR_PICKUP, READY_FOR_DELIVERY -> processing += cnt;
                case SHIPPING, OUT_FOR_DELIVERY -> shipping += cnt;
                case COMPLETED, DELIVERY_SUCCESS -> completed += cnt;
                case CANCELLED, DELIVERY_DENIED -> cancelled += cnt;
                case RETURNED, RETURN_REQUESTED -> returned += cnt;
                default -> {}
            }
        }
        long total = pending + processing + shipping + completed + cancelled + returned;
        return OrderStatusCountsResponse.builder()
                .pending(pending)
                .processing(processing)
                .shipping(shipping)
                .completed(completed)
                .cancelled(cancelled)
                .returned(returned)
                .total(total)
                .build();
    }

    @Override
    public OrdersByDayResponse getOrdersByDay(UUID storeId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(6);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        var raw = storeStatsRepository.countByDay(storeId, start, end);
        java.util.Map<LocalDate, Long> map = new java.util.HashMap<>();
        for (Object[] row : raw) {
            Object dObj = row[0];
            LocalDate d;
            if (dObj instanceof java.sql.Date sqlDate) {
                d = sqlDate.toLocalDate();
            } else if (dObj instanceof LocalDate ld) {
                d = ld;
            } else if (dObj instanceof java.time.LocalDateTime ldt) {
                d = ldt.toLocalDate();
            } else {
                d = LocalDate.parse(dObj.toString());
            }
            long cnt = (Long) row[1];
            map.put(d, cnt);
        }
        java.util.List<OrdersByDayPoint> points = new java.util.ArrayList<>();
        LocalDate cursor = from;
        long total = 0;
        while (!cursor.isAfter(to)) {
            long c = map.getOrDefault(cursor, 0L);
            total += c;
            points.add(OrdersByDayPoint.builder().date(cursor).count(c).build());
            cursor = cursor.plusDays(1);
        }
        return OrdersByDayResponse.builder().points(points).total(total).build();
    }

    @Override
    public OrderStatusRatioResponse getOrderStatusRatio(UUID storeId, LocalDate fromDate, LocalDate toDate) {
        OrderStatusCountsResponse counts = getOrderStatusCounts(storeId, fromDate, toDate);
        double total = counts.getTotal();
        if (total <= 0) {
            return OrderStatusRatioResponse.builder().pendingRatio(0).processingRatio(0).shippingRatio(0)
                    .completedRatio(0).cancelledRatio(0).returnedRatio(0).build();
        }
        java.util.function.DoubleUnaryOperator r = v -> Math.round((v / total) * 10000.0) / 100.0; // làm tròn 2 chữ số %
        return OrderStatusRatioResponse.builder()
                .pendingRatio(r.applyAsDouble(counts.getPending()))
                .processingRatio(r.applyAsDouble(counts.getProcessing()))
                .shippingRatio(r.applyAsDouble(counts.getShipping()))
                .completedRatio(r.applyAsDouble(counts.getCompleted()))
                .cancelledRatio(r.applyAsDouble(counts.getCancelled()))
                .returnedRatio(r.applyAsDouble(counts.getReturned()))
                .build();
    }

    @Override
    public ProfitStatsResponse getProfitStats(UUID storeId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(29); // mặc định 30 ngày gần nhất
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();

        BigDecimal totalRevenue = zeroIfNull(storeStatsRepository.sumRevenueRange(storeId, start, end));
        BigDecimal totalCost = zeroIfNull(storeStatsRepository.sumCostRange(storeId, start, end));
        BigDecimal platformFee = zeroIfNull(storeStatsRepository.sumPlatformFeeRange(storeId, start, end));
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal netProfit = grossProfit.subtract(platformFee);

        if (grossProfit.compareTo(BigDecimal.ZERO) < 0) grossProfit = BigDecimal.ZERO;
        if (netProfit.compareTo(BigDecimal.ZERO) < 0) netProfit = BigDecimal.ZERO;

        return ProfitStatsResponse.builder()
                .totalRevenue(scale(totalRevenue))
                .totalCost(scale(totalCost))
                .grossProfit(scale(grossProfit))
                .platformFee(scale(platformFee))
                .netProfit(scale(netProfit))
                .build();
    }

    @Override
    public java.util.List<TopSellingProductResponse> getTopSellingProducts(UUID storeId, LocalDate fromDate, LocalDate toDate, int limit) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(29);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();

        PageRequest pageable = PageRequest.of(0, limit > 0 ? limit : 10);
        var raw = storeStatsRepository.findTopSellingProducts(storeId, start, end, pageable);

        java.util.List<TopSellingProductResponse> result = new java.util.ArrayList<>();
        for (Object[] row : raw) {
            UUID productId = (UUID) row[0];
            Long totalSold = (Long) row[1];
            BigDecimal totalRevenue = (BigDecimal) row[2];

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) continue;

            String imageUrl = (product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0) : null;

            result.add(TopSellingProductResponse.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .sku(product.getSku())
                    .imageUrl(imageUrl)
                    .price(product.getPrice())
                    .totalSold(totalSold)
                    .totalRevenue(scale(totalRevenue))
                    .stockQuantity(product.getStockQuantity())
                    .build());
        }
        return result;
    }

    @Override
    public java.util.List<ProductViewAnalyticsResponse> getProductViewAnalytics(UUID storeId) {
        // Lấy danh sách sản phẩm của store
        var products = productRepository.findAll().stream()
                .filter(p -> p.getStore().getStoreId().equals(storeId))
                .collect(java.util.stream.Collectors.toList());

        // Lấy số đơn hàng theo sản phẩm
        var orderCountsRaw = storeStatsRepository.countOrdersByProduct(storeId);
        java.util.Map<UUID, Long> orderCountMap = new java.util.HashMap<>();
        for (Object[] row : orderCountsRaw) {
            UUID productId = (UUID) row[0];
            Long count = (Long) row[1];
            orderCountMap.put(productId, count);
        }

        java.util.List<ProductViewAnalyticsResponse> result = new java.util.ArrayList<>();
        for (Product p : products) {
            long viewCount = p.getViewCount() != null ? p.getViewCount() : 0;
            long orderCount = orderCountMap.getOrDefault(p.getProductId(), 0L);
            double conversionRate = viewCount > 0 ? (orderCount / (double) viewCount * 100.0) : 0.0;

            String imageUrl = (p.getImages() != null && !p.getImages().isEmpty())
                    ? p.getImages().get(0) : null;

            result.add(ProductViewAnalyticsResponse.builder()
                    .productId(p.getProductId())
                    .productName(p.getName())
                    .sku(p.getSku())
                    .imageUrl(imageUrl)
                    .viewCount(viewCount)
                    .orderCount(orderCount)
                    .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                    .build());
        }

        // Sắp xếp theo viewCount giảm dần
        result.sort((a, b) -> Long.compare(b.getViewCount(), a.getViewCount()));
        return result;
    }

    @Override
    public java.util.List<OutOfStockProductResponse> getOutOfStockProducts(UUID storeId, Integer threshold) {
        int thresholdValue = threshold != null && threshold > 0 ? threshold : 10;

        var products = productRepository.findAll().stream()
                .filter(p -> p.getStore().getStoreId().equals(storeId))
                .filter(p -> {
                    Integer stock = p.getStockQuantity();
                    return stock == null || stock <= thresholdValue;
                })
                .collect(java.util.stream.Collectors.toList());

        java.util.List<OutOfStockProductResponse> result = new java.util.ArrayList<>();
        for (Product p : products) {
            Integer stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            String status = stock <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";

            String imageUrl = (p.getImages() != null && !p.getImages().isEmpty())
                    ? p.getImages().get(0) : null;

            result.add(OutOfStockProductResponse.builder()
                    .productId(p.getProductId())
                    .productName(p.getName())
                    .sku(p.getSku())
                    .imageUrl(imageUrl)
                    .price(p.getPrice())
                    .stockQuantity(stock)
                    .threshold(thresholdValue)
                    .status(status)
                    .build());
        }

        // Sắp xếp: hết hàng trước, sau đó theo stock tăng dần
        result.sort((a, b) -> {
            if (a.getStatus().equals("OUT_OF_STOCK") && !b.getStatus().equals("OUT_OF_STOCK")) return -1;
            if (!a.getStatus().equals("OUT_OF_STOCK") && b.getStatus().equals("OUT_OF_STOCK")) return 1;
            return Integer.compare(a.getStockQuantity(), b.getStockQuantity());
        });

        return result;
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private double round(double val, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(val * factor) / factor;
    }

    private BigDecimal scale(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
}
