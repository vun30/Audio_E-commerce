package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PlatformUserStoreGrowthPoint;
import org.example.audio_ecommerce.dto.response.PlatformUserStoreOverviewResponse;
import org.example.audio_ecommerce.entity.Enum.RoleEnum; // đổi đúng enum Role của bạn
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.PlatformStatsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlatformStatsServiceImpl implements PlatformStatsService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    private static final DateTimeFormatter MYSQL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PlatformUserStoreOverviewResponse getUserStoreOverview(Integer year, Integer month) {

        // ===== Tổng ALL =====
        long totalCustomerAccounts = accountRepository.countByRole(RoleEnum.CUSTOMER); // chỉ dùng Account + Role
        long totalStores = storeRepository.count();                                // chỉ dùng Store

        // ===== Tháng cần tính (không truyền => tháng hiện tại) =====
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.from(LocalDate.now());

        YearMonth prev = ym.minusMonths(1);

        // ===== Range [from, to) =====
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        LocalDateTime fromPrev = prev.atDay(1).atStartOfDay();
        LocalDateTime toPrev = prev.plusMonths(1).atDay(1).atStartOfDay();

        long newCustomersInMonth = customerRepository.countNewCustomersInRange(from.format(MYSQL_DT), to.format(MYSQL_DT));
        long newStoresInMonth = storeRepository.countNewStoresInRange(from.format(MYSQL_DT), to.format(MYSQL_DT));

        long newCustomersPrevMonth = customerRepository.countNewCustomersInRange(fromPrev.format(MYSQL_DT), toPrev.format(MYSQL_DT));
        long newStoresPrevMonth = storeRepository.countNewStoresInRange(fromPrev.format(MYSQL_DT), toPrev.format(MYSQL_DT));

        BigDecimal customerGrowthPercent = growthPercent(newCustomersInMonth, newCustomersPrevMonth);
        BigDecimal storeGrowthPercent = growthPercent(newStoresInMonth, newStoresPrevMonth);

        return PlatformUserStoreOverviewResponse.builder()
                .totalCustomerAccounts(totalCustomerAccounts)
                .totalStores(totalStores)
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .newCustomersInMonth(newCustomersInMonth)
                .newStoresInMonth(newStoresInMonth)
                .newCustomersPrevMonth(newCustomersPrevMonth)
                .newStoresPrevMonth(newStoresPrevMonth)
                .customerGrowthPercent(customerGrowthPercent)
                .storeGrowthPercent(storeGrowthPercent)
                .build();
    }

    @Override
    public List<PlatformUserStoreGrowthPoint> getUserStoreGrowthChartByYear(Integer year) {

        int y = (year != null) ? year : LocalDate.now().getYear();

        Map<Integer, Long> cusByMonth = new HashMap<>();
        for (Object[] r : customerRepository.countNewCustomersByMonth(y)) {
            int m = ((Number) r[0]).intValue();
            long cnt = ((Number) r[1]).longValue();
            cusByMonth.put(m, cnt);
        }

        Map<Integer, Long> storeByMonth = new HashMap<>();
        for (Object[] r : storeRepository.countNewStoresByMonth(y)) {
            int m = ((Number) r[0]).intValue();
            long cnt = ((Number) r[1]).longValue();
            storeByMonth.put(m, cnt);
        }

        List<PlatformUserStoreGrowthPoint> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            result.add(PlatformUserStoreGrowthPoint.builder()
                    .year(y)
                    .month(m)
                    .newCustomers(cusByMonth.getOrDefault(m, 0L))
                    .newStores(storeByMonth.getOrDefault(m, 0L))
                    .build());
        }
        return result;
    }

    // ===== helper =====
    private static BigDecimal growthPercent(long current, long prev) {
        if (prev == 0) {
            return current == 0 ? BigDecimal.ZERO : new BigDecimal("100");
        }
        return BigDecimal.valueOf(current - prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 2, RoundingMode.HALF_UP);
    }
}
