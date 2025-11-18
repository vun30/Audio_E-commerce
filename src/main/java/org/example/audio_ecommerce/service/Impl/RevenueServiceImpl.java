package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;
import org.example.audio_ecommerce.entity.PlatformRevenue;
import org.example.audio_ecommerce.entity.StoreRevenue;
import org.example.audio_ecommerce.repository.PlatformRevenueRepository;
import org.example.audio_ecommerce.repository.StoreRevenueRepository;
import org.example.audio_ecommerce.repository.projection.PlatformRevenueAgg;
import org.example.audio_ecommerce.repository.projection.StoreRevenueAgg;
import org.example.audio_ecommerce.service.RevenueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueServiceImpl implements RevenueService {

    private final StoreRevenueRepository storeRevenueRepo;
    private final PlatformRevenueRepository platformRevenueRepo;

    // =================== Helpers ===================

    private LocalDate defaultFrom(LocalDate from) {
        return from != null ? from : LocalDate.now().minusMonths(1);
    }

    private LocalDate defaultTo(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }

    private StoreRevenueResponse toStoreRevenueResponse(StoreRevenue sr) {
        return StoreRevenueResponse.builder()
                .id(sr.getId())
                .storeId(sr.getStoreId())
                .storeOrderId(sr.getStoreOrderId())
                .amount(sr.getAmount())
                .feePlatform(sr.getFeePlatform())
                .feeShipping(sr.getFeeShipping())
                .revenueDate(sr.getRevenueDate())
                .createdAt(sr.getCreatedAt())
                .build();
    }

    private PlatformRevenueResponse toPlatformRevenueResponse(PlatformRevenue pr) {
        return PlatformRevenueResponse.builder()
                .id(pr.getId())
                .storeOrderId(pr.getStoreOrderId())
                .type(pr.getType())
                .amount(pr.getAmount())
                .revenueDate(pr.getRevenueDate())
                .createdAt(pr.getCreatedAt())
                .build();
    }

    // =================== Record ===================

    @Override
    @Transactional
    public void recordStoreRevenue(UUID storeId,
                                   UUID storeOrderId,
                                   BigDecimal amount,
                                   BigDecimal feePlatform,
                                   BigDecimal feeShipping,
                                   LocalDate revenueDate) {

        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        StoreRevenue sr = StoreRevenue.builder()
                .storeId(storeId)
                .storeOrderId(storeOrderId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .feePlatform(feePlatform != null ? feePlatform : BigDecimal.ZERO)
                .feeShipping(feeShipping != null ? feeShipping : BigDecimal.ZERO)
                .revenueDate(revenueDate != null ? revenueDate : LocalDate.now())
                .build();
        storeRevenueRepo.save(sr);
        log.info("Recorded store revenue: storeId={}, storeOrderId={}, amount={}, platformFee={}, shippingFee={}",
                storeId, storeOrderId, amount, feePlatform, feeShipping);
    }

    @Override
    @Transactional
    public void recordPlatformRevenue(UUID storeOrderId,
                                      PlatformRevenueType type,
                                      BigDecimal amount,
                                      LocalDate revenueDate) {

        if (type == null) {
            throw new IllegalArgumentException("PlatformRevenueType is required");
        }
        PlatformRevenue pr = PlatformRevenue.builder()
                .storeOrderId(storeOrderId)
                .type(type)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .revenueDate(revenueDate != null ? revenueDate : LocalDate.now())
                .build();
        platformRevenueRepo.save(pr);
        log.info("Recorded platform revenue: type={}, storeOrderId={}, amount={}",
                type, storeOrderId, amount);
    }

    // =================== Query: Store ===================

    @Override
    @Transactional(readOnly = true)
    public Page<StoreRevenueResponse> getStoreRevenue(UUID storeId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      Pageable pageable) {

        LocalDate from = defaultFrom(fromDate);
        LocalDate to = defaultTo(toDate);

        Page<StoreRevenue> page = storeRevenueRepo
                .findByStoreIdAndRevenueDateBetween(storeId, from, to, pageable);

        return page.map(this::toStoreRevenueResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreRevenueSummaryResponse getStoreRevenueSummary(UUID storeId,
                                                              LocalDate fromDate,
                                                              LocalDate toDate) {

        LocalDate from = defaultFrom(fromDate);
        LocalDate to = defaultTo(toDate);

        StoreRevenueAgg agg = storeRevenueRepo.aggregateByStoreAndDate(storeId, from, to);

        BigDecimal totalAmount = agg != null && agg.getTotalAmount() != null
                ? agg.getTotalAmount()
                : BigDecimal.ZERO;
        BigDecimal totalPlatformFee = agg != null && agg.getTotalPlatformFee() != null
                ? agg.getTotalPlatformFee()
                : BigDecimal.ZERO;
        BigDecimal totalShippingFee = agg != null && agg.getTotalShippingFee() != null
                ? agg.getTotalShippingFee()
                : BigDecimal.ZERO;

        return StoreRevenueSummaryResponse.builder()
                .storeId(storeId)
                .fromDate(from)
                .toDate(to)
                .totalAmount(totalAmount)
                .totalPlatformFee(totalPlatformFee)
                .totalShippingFee(totalShippingFee)
                .build();
    }

    // =================== Query: Platform ===================

    @Override
    @Transactional(readOnly = true)
    public Page<PlatformRevenueResponse> getPlatformRevenue(LocalDate fromDate,
                                                            LocalDate toDate,
                                                            Pageable pageable) {

        LocalDate from = defaultFrom(fromDate);
        LocalDate to = defaultTo(toDate);

        Page<PlatformRevenue> page = platformRevenueRepo
                .findByRevenueDateBetween(from, to, pageable);

        return page.map(this::toPlatformRevenueResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueSummaryResponse getPlatformRevenueSummary(LocalDate fromDate,
                                                                    LocalDate toDate) {

        LocalDate from = defaultFrom(fromDate);
        LocalDate to = defaultTo(toDate);

        List<PlatformRevenueAgg> aggs = platformRevenueRepo.aggregateByTypeAndDate(from, to);

        Map<PlatformRevenueType, BigDecimal> totalByType = new EnumMap<>(PlatformRevenueType.class);
        for (PlatformRevenueType t : PlatformRevenueType.values()) {
            totalByType.put(t, BigDecimal.ZERO);
        }

        for (PlatformRevenueAgg agg : aggs) {
            PlatformRevenueType type = agg.getType();
            BigDecimal total = agg.getTotalAmount() != null ? agg.getTotalAmount() : BigDecimal.ZERO;
            totalByType.put(type, total);
        }

        BigDecimal totalAll = totalByType.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PlatformRevenueSummaryResponse.builder()
                .fromDate(from)
                .toDate(to)
                .totalByType(totalByType)
                .totalAll(totalAll)
                .build();
    }
}
