//package org.example.audio_ecommerce.service.Impl;
//
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.response.PlatformPayoutRevenueStatsResponse;
//import org.example.audio_ecommerce.repository.PlatformPayoutStatsRepository;
//import org.example.audio_ecommerce.service.PlatformPayoutStatsService;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.YearMonth;
//import java.time.format.DateTimeFormatter;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class PlatformPayoutStatsServiceImpl implements PlatformPayoutStatsService {
//
//    private final PlatformPayoutStatsRepository payoutStatsRepo;
//
//    private static final DateTimeFormatter MYSQL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//    @Override
//    public PlatformPayoutRevenueStatsResponse getPayoutRevenueStats(Integer year, Integer month, List<UUID> storeIds) {
//
//        YearMonth ym = (year != null && month != null)
//                ? YearMonth.of(year, month)
//                : YearMonth.from(LocalDate.now());
//
//        LocalDateTime from = ym.atDay(1).atStartOfDay();
//        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
//
//        List<UUID> safeStoreIds = (storeIds == null) ? Collections.emptyList() : storeIds;
//
//        Object[] r;
//        if (safeStoreIds.isEmpty()) {
//            // ✅ Không filter store
//            r = payoutStatsRepo.payoutRevenueStatsAllStores(
//                    from.format(MYSQL_DT),
//                    to.format(MYSQL_DT)
//            );
//        } else {
//            // ✅ Filter theo danh sách storeIds
//            r = payoutStatsRepo.payoutRevenueStatsByStores(
//                    from.format(MYSQL_DT),
//                    to.format(MYSQL_DT),
//                    safeStoreIds
//            );
//        }
//
//        long eligibleItemCount = toLong(r[0]);
//        long eligibleOrderCount = toLong(r[1]);
//        BigDecimal eligibleGross = toBig(r[2]);
//        BigDecimal platformFeeCollected = toBig(r[3]);
//
//        // ✅ avg fee per item
//        BigDecimal avgPlatformFeePerItem =
//                eligibleItemCount == 0
//                        ? BigDecimal.ZERO
//                        : platformFeeCollected.divide(BigDecimal.valueOf(eligibleItemCount), 2, RoundingMode.HALF_UP);
//
//        // ✅ weighted avg fee rate (%)
//        BigDecimal avgPlatformFeeRatePercent =
//                eligibleGross.compareTo(BigDecimal.ZERO) == 0
//                        ? BigDecimal.ZERO
//                        : platformFeeCollected
//                        .multiply(BigDecimal.valueOf(100))
//                        .divide(eligibleGross, 6, RoundingMode.HALF_UP);
//
//        return PlatformPayoutRevenueStatsResponse.builder()
//                .year(ym.getYear())
//                .month(ym.getMonthValue())
//                .storeIds(safeStoreIds)
//                .from(from.toString())
//                .to(to.toString())
//                .eligibleItemCount(eligibleItemCount)
//                .eligibleOrderCount(eligibleOrderCount)
//                .eligibleGross(eligibleGross)
//                .platformFeeCollected(platformFeeCollected)
//                .avgPlatformFeePerItem(avgPlatformFeePerItem)
//                .avgPlatformFeeRatePercent(avgPlatformFeeRatePercent) // ✅ thêm field này trong DTO
//                .build();
//    }
//
//    private static long toLong(Object o) {
//        if (o == null) return 0L;
//        if (o instanceof Number n) return n.longValue();
//        return Long.parseLong(o.toString());
//    }
//
//    private static BigDecimal toBig(Object o) {
//        if (o == null) return BigDecimal.ZERO;
//        if (o instanceof BigDecimal b) return b;
//        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
//        return new BigDecimal(o.toString());
//    }
//}
