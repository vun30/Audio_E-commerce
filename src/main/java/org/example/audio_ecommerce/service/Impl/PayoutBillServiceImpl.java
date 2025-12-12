package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.PayoutGroup;
import org.example.audio_ecommerce.dto.response.PayoutItemDetail;
import org.example.audio_ecommerce.dto.response.PayoutOverviewResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.PayoutBillService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutBillServiceImpl implements PayoutBillService {

    private final StoreOrderItemRepository storeOrderItemRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreRepository storeRepository;

    private final PayoutBillRepository payoutBillRepository;
    private final PayoutBillItemRepository payoutBillItemRepository;
    private final PayoutShippingOrderFeeRepository payoutShippingOrderFeeRepository;
    private final PayoutReturnShippingFeeRepository payoutReturnShippingFeeRepository;

    // ===================================================================
    // 1) TẠO BILL MỚI
    // ===================================================================
    @Override
    public PayoutBill createBillForShop(UUID storeId) {

        // ❗ STOP nếu shop có bill chưa thanh toán
        validateNoUnpaidBill(storeId);

        PayoutBill created = PayoutBill.builder()
                .shopId(storeId)
                .billCode(generateBillCode())
                .fromDate(LocalDateTime.now().minusDays(7))
                .toDate(LocalDateTime.now())
                .status(PayoutBillStatus.PENDING)
                .items(new ArrayList<>())
                .shippingOrders(new ArrayList<>())
                .returnShipFees(new ArrayList<>())
                .build();

        final PayoutBill bill = payoutBillRepository.save(created);

        // ===============================================================
        // BILL ITEMS
        // ===============================================================
        List<StoreOrderItem> orderItems =
                storeOrderItemRepository
                        .findAllByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(storeId);

        List<PayoutBillItem> billItems = orderItems.stream()
                .map(item -> toBillItem(item, bill))
                .toList();

        payoutBillItemRepository.saveAll(billItems);
        bill.getItems().addAll(billItems);

        // ===============================================================
        // SHIPPING FEE (CHỈ LẤY PHÍ CHÊNH CHƯA TRẢ)
        // ===============================================================
        List<StoreOrder> orders = storeOrderRepository.findPendingShippingOrders(storeId);

        List<PayoutShippingOrderFee> shippingFees = orders.stream()
                .map(o -> toShippingFee(o, bill))
                .toList();

        payoutShippingOrderFeeRepository.saveAll(shippingFees);
        bill.getShippingOrders().addAll(shippingFees);

        // ===============================================================
        // RETURN SHIPPING FEE
        // ===============================================================
        List<ReturnShippingFee> returnFeesEntity =
                returnShippingFeeRepository.findAllByStoreIdAndPaidByShopFalse(storeId);

        List<PayoutReturnShippingFee> returnFees = returnFeesEntity.stream()
                .map(r -> toReturnShippingFee(r, bill))
                .toList();

        payoutReturnShippingFeeRepository.saveAll(returnFees);
        bill.getReturnShipFees().addAll(returnFees);

        // ===============================================================
        // TÍNH TỔNG BILL
        // ===============================================================
        computeBillTotals(bill);

        return payoutBillRepository.save(bill);
    }

    private String generateBillCode() {
        return "PB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ===================================================================
    // MAPPING
    // ===================================================================
    private PayoutBillItem toBillItem(StoreOrderItem item, PayoutBill bill) {

        return PayoutBillItem.builder()
                .bill(bill)
                .orderItemId(item.getId())
                .storeOrderId(item.getStoreOrder().getId())
                .productName("Product-" + item.getId())
                .quantity(item.getQuantity())
                .finalLineTotal(item.getFinalLineTotal() == null ? BigDecimal.ZERO : item.getFinalLineTotal())
                .platformFeePercentage(item.getPlatformFeePercentage() == null ? BigDecimal.ZERO : item.getPlatformFeePercentage())
                .isReturned(item.getIsReturned() != null && item.getIsReturned())
                .build();
    }

    private PayoutShippingOrderFee toShippingFee(StoreOrder order, PayoutBill bill) {

        BigDecimal fee = order.getShippingFeeForStore() == null
                ? BigDecimal.ZERO
                : order.getShippingFeeForStore();

        return PayoutShippingOrderFee.builder()
                .bill(bill)
                .storeOrderId(order.getId())
                .ghnOrderCode(order.getOrderCode())
                .shippingFee(fee)
                .shippingType("SHIPPING")
                .build();
    }

    private PayoutReturnShippingFee toReturnShippingFee(ReturnShippingFee fee, PayoutBill bill) {

        return PayoutReturnShippingFee.builder()
                .bill(bill)
                .returnRequestId(fee.getReturnRequestId())
                .ghnOrderCode(fee.getGhnOrderCode())
                .shippingFee(fee.getShippingFee())
                .chargedToShop(fee.getChargedToShop())
                .shippingType("RETURN")
                .build();
    }

    // ===================================================================
    // TÍNH TỔNG BILL
    // ===================================================================
    private void computeBillTotals(PayoutBill bill) {

        BigDecimal totalGross = bill.getItems().stream()
                .map(PayoutBillItem::getFinalLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFee = bill.getItems().stream()
                .map(PayoutBillItem::getPlatformFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalShippingFee = bill.getShippingOrders().stream()
                .map(PayoutShippingOrderFee::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnFee = bill.getReturnShipFees().stream()
                .map(PayoutReturnShippingFee::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = totalGross
                .subtract(totalPlatformFee)
                .subtract(totalShippingFee)
                .subtract(totalReturnFee);

        bill.setTotalGross(totalGross);
        bill.setTotalPlatformFee(totalPlatformFee);
        bill.setTotalShippingOrderFee(totalShippingFee);
        bill.setTotalReturnShippingFee(totalReturnFee);
        bill.setTotalNetPayout(totalNet);
    }

    // ===================================================================
    // MARK BILL PAID
    // ===================================================================
 @Override
public PayoutBill markBillAsPaid(UUID billId, String reference, String receiptUrl, String note) {

    PayoutBill bill = payoutBillRepository.findById(billId)
            .orElseThrow(() -> new RuntimeException("Bill not found"));

    bill.setTransferReference(reference);
    bill.setReceiptImageUrl(receiptUrl);
    bill.setAdminNote(note);
    bill.setStatus(PayoutBillStatus.PAID);

    UUID storeId = bill.getShopId();

    // =====================================================
    // 1) SET ReturnShippingFee.paidByShop = true (THEO STORE)
    // =====================================================
    List<ReturnShippingFee> returnFees =
            returnShippingFeeRepository.findAllByStoreIdAndPaidByShopFalse(storeId);

    returnFees.forEach(f -> {
        f.setPaidByShop(true);
        returnShippingFeeRepository.save(f);
    });

    // =====================================================
    // 2) SET StoreOrder.paidByShop = true (THEO STORE)
    // =====================================================
    bill.getShippingOrders().forEach(s -> {
        StoreOrder order = storeOrderRepository
                .findById(s.getStoreOrderId())
                .orElseThrow();

        order.setPaidByShop(true);
        storeOrderRepository.save(order);
    });

    // =====================================================
    // 3) SET StoreOrderItem.isPayout = true (THEO STORE)
    // =====================================================
    bill.getItems().forEach(i -> {
        StoreOrderItem item = storeOrderItemRepository
                .findById(i.getOrderItemId())
                .orElseThrow();

        item.setIsPayout(true);
        storeOrderItemRepository.save(item);
    });

    return payoutBillRepository.save(bill);
}


    @Override
public List<PayoutBill> autoCreateBillsForAllStores() {

    List<UUID> storeIds = storeRepository.findAllStoreIds();
    List<PayoutBill> res = new ArrayList<>();

    for (UUID storeId : storeIds) {

        // ❗ Bỏ qua shop có bill chưa thanh toán
        boolean hasUnpaid =
                payoutBillRepository.existsByShopIdAndStatusNot(storeId, PayoutBillStatus.PAID);

        if (hasUnpaid) {
            System.out.println("⛔ SKIP shop " + storeId + " — đang có bill chưa thanh toán.");
            continue;
        }

        boolean hasOrderItems =
                storeOrderItemRepository
                        .existsByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(storeId);

        boolean hasShippingFees =
                storeOrderRepository.findPendingShippingOrders(storeId).size() > 0;

        boolean hasReturnFees =
                returnShippingFeeRepository.existsByStoreIdAndPaidByShopFalse(storeId);

        if (!hasOrderItems && !hasShippingFees && !hasReturnFees)
            continue;

        res.add(createBillForShop(storeId)); // Không còn throw lỗi trong createBillForShop()
    }

    return res;
}

    @Override
    public PayoutBill getOrCreateBillForStore(UUID storeId) {

        PayoutBill existing = payoutBillRepository
                .findFirstByShopIdAndStatusOrderByCreatedAtDesc(storeId, PayoutBillStatus.PENDING);

        if (existing != null)
            return getFullBill(existing.getId());

        boolean hasOrderItems =
                storeOrderItemRepository
                        .existsByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(storeId);

        boolean hasShippingFees =
                storeOrderRepository.findPendingShippingOrders(storeId).size() > 0;

        boolean hasReturnFees =
                returnShippingFeeRepository.existsByStoreIdAndPaidByShopFalse(storeId);

        if (!hasOrderItems && !hasShippingFees && !hasReturnFees)
            throw new RuntimeException("Store này không có dữ liệu để tạo bill payout.");

        return createBillForShop(storeId);
    }

    @Override
    public PayoutBill getFullBill(UUID billId) {
        return payoutBillRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
    }

    @Override
    public List<PayoutBill> listBills(UUID storeId,
                                      PayoutBillStatus status,
                                      LocalDateTime fromDate,
                                      LocalDateTime toDate,
                                      String billCode) {

        return payoutBillRepository.filterBills(
                storeId,
                status,
                fromDate,
                toDate,
                billCode
        );
    }


    private void validateNoUnpaidBill(UUID storeId) {
        boolean exists = payoutBillRepository
                .existsByShopIdAndStatusNot(storeId, PayoutBillStatus.PAID);

        if (exists) {
            throw new RuntimeException(
                    "❌ Shop hiện đang có bill payout CHƯA THANH TOÁN. Vui lòng thanh toán bill cũ trước khi tạo bill mới."
            );
        }
    }

    public PayoutOverviewResponse getOverview(UUID storeId, LocalDateTime from, LocalDateTime to) {

        return PayoutOverviewResponse.builder()
                .undeliCOD(buildGroup(storeId, false, PaymentMethod.COD, from, to))
                .undeliONLINE(buildGroup(storeId, false, PaymentMethod.ONLINE, from, to))
                .deliCOD(buildDeliveredGroup(storeId, PaymentMethod.COD, from, to))
                .deliONLINE(buildDeliveredGroup(storeId, PaymentMethod.ONLINE, from, to))
                .platformFee(buildPlatformFee(storeId, from, to))
                .totalPaid(buildTotalPaid(storeId, from, to))
                .build();
    }

    // ============================================================
    //     GROUP: Undelivered COD / Undelivered ONLINE
    // ============================================================
    private PayoutGroup buildGroup(UUID storeId,
                                   boolean isDelivered,
                                   PaymentMethod payment,
                                   LocalDateTime from,
                                   LocalDateTime to) {

        List<StoreOrderItem> items =
                storeOrderItemRepository.findItemsForOverview(storeId, isDelivered, payment, false, from, to);

        return mapToGroup(items);
    }

    // ============================================================
    //     GROUP: Delivered COD / Delivered ONLINE
    // ============================================================
    private PayoutGroup buildDeliveredGroup(UUID storeId,
                                            PaymentMethod payment,
                                            LocalDateTime from,
                                            LocalDateTime to) {

        List<StoreOrderItem> items =
                storeOrderItemRepository.findItemsForOverview(storeId, true, payment, false, from, to);

        return mapToGroup(items);
    }

    // ============================================================
    //     PLATFORM FEE
    // ============================================================
    private PayoutGroup buildPlatformFee(UUID storeId, LocalDateTime from, LocalDateTime to) {

        List<StoreOrderItem> items =
                storeOrderItemRepository.findPlatformFeeItems(storeId, from, to);

        return mapToGroup(items);
    }

    // ============================================================
    //     TOTAL PAID TO STORE
    // ============================================================
    private PayoutGroup buildTotalPaid(UUID storeId, LocalDateTime from, LocalDateTime to) {

        List<StoreOrderItem> items =
                storeOrderItemRepository.findPaidItems(storeId, from, to);

        return mapToGroup(items);
    }

    // ============================================================
    //     MAPPING ITEM → GROUP RESPONSE
    // ============================================================
    private PayoutGroup mapToGroup(List<StoreOrderItem> items) {

        BigDecimal total = BigDecimal.ZERO;

        List<PayoutItemDetail> list = new ArrayList<>();

        for (StoreOrderItem i : items) {

            BigDecimal shipEst = i.getShippingFeeEstimated() == null ? BigDecimal.ZERO : i.getShippingFeeEstimated();
            BigDecimal shipActual = i.getShippingFeeActual() == null ? BigDecimal.ZERO : i.getShippingFeeActual();
            BigDecimal returnFee = i.getShippingExtraForStore() == null ? BigDecimal.ZERO : i.getShippingExtraForStore();

            BigDecimal baseAmount = i.getFinalLineTotal().subtract(shipEst);
            BigDecimal fee = baseAmount
                    .multiply(i.getPlatformFeePercentage())
                    .divide(BigDecimal.valueOf(100));

            BigDecimal net = i.getFinalLineTotal()
                    .subtract(shipActual)
                    .subtract(returnFee)
                    .subtract(fee);

            total = total.add(net);

            list.add(PayoutItemDetail.builder()
                    .itemId(i.getId())
                    .productName(i.getName())
                    .quantity(i.getQuantity())
                    .finalLineTotal(i.getFinalLineTotal())
                    .shippingFeeEstimated(shipEst)
                    .shippingFeeActual(shipActual)
                    .returnShippingFee(returnFee)
                    .platformFeePercentage(i.getPlatformFeePercentage())
                    .platformFeeAmount(fee)
                    .netAmount(net)
                    .deliveredAt(i.getDeliveredAt())
                    .build());
        }

        return PayoutGroup.builder()
                .countItems(items.size())
                .totalAmount(total)
                .items(list)
                .build();
    }
}
