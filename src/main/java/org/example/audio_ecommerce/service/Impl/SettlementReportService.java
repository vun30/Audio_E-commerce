package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.ItemContribution;
import org.example.audio_ecommerce.dto.response.SettlementReportResponse;
import org.example.audio_ecommerce.dto.response.StoreOrderReportEntry;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.SettlementReportType;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SettlementReportService {

    private final StoreOrderRepository storeOrderRepository;

    // Helper: compute item-level platform fee and net payout (per your rule)
    // platform fee per item = (item.lineTotal - itemShippingShare) * pct/100
    // We'll apportion customer shipping fee to items proportionally by lineTotal/totalProducts
   private ItemContribution buildItemContribution(StoreOrder so, StoreOrderItem item) {

    BigDecimal lineTotal = Optional.ofNullable(item.getLineTotal()).orElse(BigDecimal.ZERO);
    BigDecimal platformPct = Optional.ofNullable(so.getPlatformFeePercentage()).orElse(BigDecimal.ZERO);
    BigDecimal shippingExtraForStore = Optional.ofNullable(item.getShippingExtraForStore()).orElse(BigDecimal.ZERO);

    // PLATFORM FEE = lineTotal * pct / 100
    BigDecimal platformFeeAmount = lineTotal
            .multiply(platformPct)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    // NET = lineTotal - platformFee - shippingExtra
    BigDecimal netPayoutItem = lineTotal
            .subtract(platformFeeAmount)
            .subtract(shippingExtraForStore);

    return ItemContribution.builder()
            .itemId(item.getId())
            .storeOrderId(so.getId())
            .productName(item.getName())
            .quantity(item.getQuantity())
            .lineTotal(lineTotal)
            .shippingFeeEstimated(item.getShippingFeeEstimated())
            .shippingFeeActual(item.getShippingFeeActual())
            .shippingExtraForStore(shippingExtraForStore)
            .platformFeePercentage(platformPct)
            .platformFeeAmount(platformFeeAmount)
            .netPayoutItem(netPayoutItem)
            .build();
}

    public SettlementReportResponse getReport(SettlementReportType type, LocalDate date, UUID storeId) {
        List<StoreOrder> storeOrders = new ArrayList<>();
        LocalDateTime from = null, to = null;
        if (date != null) {
            from = date.atStartOfDay();
            to = date.plusDays(1).atStartOfDay();
        }

        switch (type) {
            case UNDELI_COD:
                storeOrders = storeOrderRepository.findUnDeliveredByPaymentMethodFetchItems(PaymentMethod.COD);
                break;
            case UNDELI_ONLINE:
                storeOrders = storeOrderRepository.findUnDeliveredByPaymentMethodFetchItems(PaymentMethod.ONLINE);
                break;
            case DELI_COD:
            case DELI_ONLINE:
                if (from == null) {
                    // date null -> lấy tất cả delivered (không filter theo ngày). Nếu cần filter theo storeId, thực hiện sau.
                    storeOrders = storeOrderRepository.findAllWithItemsFetch().stream()
                            .filter(so -> so.getDeliveredAt() != null) // chỉ lấy delivered
                            .collect(Collectors.toList());
                } else {
                    storeOrders = (storeId == null)
                            ? storeOrderRepository.findDeliveredBetweenFetchItems(from, to)
                            : storeOrderRepository.findDeliveredBetweenByStoreFetchItems(storeId, from, to);
                }
                break;
            case PLATFORM_FEE_TO_COLLECT:
            case TOTAL_COLLECTED:
                if (from == null) {
                    // date null -> lấy tất cả delivered (tính platform fee hoặc total collected sao cho phù hợp)
                    storeOrders = storeOrderRepository.findAllWithItemsFetch().stream()
                            .filter(so -> so.getDeliveredAt() != null)
                            .collect(Collectors.toList());
                } else {
                    storeOrders = (storeId == null)
                            ? storeOrderRepository.findDeliveredBetweenFetchItems(from, to)
                            : storeOrderRepository.findDeliveredBetweenByStoreFetchItems(storeId, from, to);
                }
                break;
            default:
                storeOrders = storeOrderRepository.findAllWithItemsFetch();
        }

        // Nếu caller truyền storeId nhưng repo trả toàn bộ, lọc theo storeId ở đây
        if (storeId != null) {
            storeOrders = storeOrders.stream()
                    .filter(so -> so.getStore() != null && storeId.equals(so.getStore().getStoreId()))
                    .collect(Collectors.toList());
        }

        List<StoreOrderReportEntry> entries = new ArrayList<>();
        BigDecimal totalAcross = BigDecimal.ZERO;

        final LocalDateTime finalFrom = from;
        final LocalDateTime finalTo = to;
        final LocalDate finalDate = date;

        for (StoreOrder so : storeOrders) {
            // derive expected payment method for the DELI/UNDELI branches as FINAL variables
            final PaymentMethod finalExpectedPmForUndel;
            final PaymentMethod finalExpectedPmForDeli;
            if (type == SettlementReportType.UNDELI_COD) finalExpectedPmForUndel = PaymentMethod.COD;
            else if (type == SettlementReportType.UNDELI_ONLINE) finalExpectedPmForUndel = PaymentMethod.ONLINE;
            else finalExpectedPmForUndel = null;

            if (type == SettlementReportType.DELI_COD) finalExpectedPmForDeli = PaymentMethod.COD;
            else if (type == SettlementReportType.DELI_ONLINE) finalExpectedPmForDeli = PaymentMethod.ONLINE;
            else finalExpectedPmForDeli = null;

            // guard null items
            if (so.getItems() == null || so.getItems().isEmpty()) continue;

            // filter items according to report typer
            List<StoreOrderItem> relevantItems = so.getItems().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> {
                        switch (type) {
                            case UNDELI_COD:
                            case UNDELI_ONLINE:
                                return !Boolean.TRUE.equals(item.getEligibleForPayout())
                                        && !Boolean.TRUE.equals(item.getPayoutProcessed())
                                        && so.getDeliveredAt() == null
                                        && finalExpectedPmForUndel != null
                                        && so.getPaymentMethod() == finalExpectedPmForUndel;
                            case DELI_COD:
                            case DELI_ONLINE:
                                if (so.getDeliveredAt() == null) return false;
                                if (finalExpectedPmForDeli != null && so.getPaymentMethod() != finalExpectedPmForDeli) return false;
                                if (finalFrom == null || finalTo == null) {
                                    // date null -> accept all delivered items that are eligible & not processed
                                    return Boolean.TRUE.equals(item.getEligibleForPayout())
                                            && !Boolean.TRUE.equals(item.getPayoutProcessed());
                                } else {
                                    return Boolean.TRUE.equals(item.getEligibleForPayout())
                                            && !Boolean.TRUE.equals(item.getPayoutProcessed())
                                            && !so.getDeliveredAt().isBefore(finalFrom)
                                            && so.getDeliveredAt().isBefore(finalTo);
                                }
                            case PLATFORM_FEE_TO_COLLECT:
                                if (so.getDeliveredAt() == null) return false;
                                if (finalFrom == null || finalTo == null) {
                                    return Boolean.TRUE.equals(item.getEligibleForPayout())
                                            && !Boolean.TRUE.equals(item.getPayoutProcessed());
                                }
                                return Boolean.TRUE.equals(item.getEligibleForPayout())
                                        && !Boolean.TRUE.equals(item.getPayoutProcessed())
                                        && !so.getDeliveredAt().isBefore(finalFrom)
                                        && so.getDeliveredAt().isBefore(finalTo);
                            case TOTAL_COLLECTED:
                                if (so.getDeliveredAt() == null) return false;
                                if (finalFrom == null || finalTo == null) {
                                    return Boolean.TRUE.equals(item.getEligibleForPayout())
                                            && Boolean.TRUE.equals(item.getPayoutProcessed());
                                }
                                return Boolean.TRUE.equals(item.getEligibleForPayout())
                                        && Boolean.TRUE.equals(item.getPayoutProcessed())
                                        && !so.getDeliveredAt().isBefore(finalFrom)
                                        && so.getDeliveredAt().isBefore(finalTo);
                            default:
                                return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (relevantItems.isEmpty()) continue;

            // aggregate numbers (unchanged)
            BigDecimal productsTotal = relevantItems.stream()
                    .map(i -> Optional.ofNullable(i.getLineTotal()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal customerShipFee = Optional.ofNullable(so.getShippingFee()).orElse(BigDecimal.ZERO);
            BigDecimal actualShipFee = Optional.ofNullable(so.getActualShippingFee()).orElse(BigDecimal.ZERO);
            BigDecimal shippingExtra = Optional.ofNullable(so.getShippingExtraForStore()).orElse(BigDecimal.ZERO);
            BigDecimal platformPct = Optional.ofNullable(so.getPlatformFeePercentage()).orElse(BigDecimal.ZERO);

            // compute per-item contributions and sum platformFee
            List<ItemContribution> itemContribs = relevantItems.stream()
                    .map(i -> buildItemContribution(so, i))
                    .collect(Collectors.toList());

            BigDecimal platformFeeTotal = itemContribs.stream()
                    .map(ItemContribution::getPlatformFeeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netPayoutTotal = itemContribs.stream()
                    .map(ItemContribution::getNetPayoutItem)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StoreOrderReportEntry entry = StoreOrderReportEntry.builder()
                    .storeOrderId(so.getId())
                    .orderCode(so.getOrderCode())
                    .storeId(so.getStore() != null ? so.getStore().getStoreId() : null)
                    .paymentMethod(so.getPaymentMethod())
                    .createdAt(so.getCreatedAt())
                    .deliveredAt(so.getDeliveredAt())
                    .productsTotal(productsTotal)
                    .customerShippingFee(customerShipFee)
                    .actualShippingFee(actualShipFee)
                    .shippingExtraForStore(shippingExtra)
                    .platformFeePercentage(platformPct)
                    .platformFeeAmount(platformFeeTotal)
                    .netPayoutToStore(netPayoutTotal)
                    .items(itemContribs)
                    .build();

            entries.add(entry);

            // totalAcross meaning depends on report type:
            switch (type) {
                case UNDELI_COD:
                case UNDELI_ONLINE:
                    totalAcross = totalAcross.add(productsTotal);
                    break;
                case DELI_COD:
                case DELI_ONLINE:
                    totalAcross = totalAcross.add(netPayoutTotal);
                    break;
                case PLATFORM_FEE_TO_COLLECT:
                    totalAcross = totalAcross.add(platformFeeTotal);
                    break;
                case TOTAL_COLLECTED:
                    totalAcross = totalAcross.add(netPayoutTotal);
                    break;
            }
        }

        return SettlementReportResponse.builder()
                .reportType(type)
                .date(finalDate)
                .entries(entries)
                .totalAmount(totalAcross)
                .build();
    }

}
