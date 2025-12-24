package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.integration.ghn.dto.GhnOrderDetail;
import org.example.audio_ecommerce.integration.ghn.dto.GhnOrderDetailWrapper;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.GhnOrderRepository;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.scheduler.StoreOrderDebtCron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnStatusSyncService {

    private final RestTemplate restTemplate;
    private final GhnOrderRepository ghnOrderRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final CustomerOrderRepository customerOrderRepo;
    private final ObjectMapper objectMapper;
    private final ReturnShippingFeeRepository returnShippingFeeRepo;
    private final SettlementService settlementService;
    private final StoreOrderDebtCron storeOrderDebtCron;

    @Value("${ghn.token}")
    private String ghnToken;

    private static final String BASE_URL =
            "https://online-gateway.ghn.vn/shiip/public-api";

    // Các status GHN cần sync định kỳ (đang hoạt động)
    private static final EnumSet<GhnStatus> ACTIVE_STATUSES = EnumSet.of(
            GhnStatus.READY_PICKUP,
            GhnStatus.READY_TO_PICK,         // Thêm status mặc định
            GhnStatus.PICKING,
            GhnStatus.MONEY_COLLECT_PICKING,
            GhnStatus.PICKED,
            GhnStatus.STORING,
            GhnStatus.TRANSPORTING,
            GhnStatus.SORTING,
            GhnStatus.DELIVERING,
            GhnStatus.MONEY_COLLECT_DELIVERING,
            GhnStatus.WAITING_TO_RETURN,
            GhnStatus.RETURN,
            GhnStatus.RETURN_TRANSPORTING,
            GhnStatus.RETURN_SORTING,
            GhnStatus.RETURNING
            // RETURNED / DELIVERED / CANCEL / LOST / DAMAGE… là trạng thái cuối → không cần spam gọi nữa
    );

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Đồng bộ trạng thái GHN cho toàn bộ đơn đang active.
     * Cron sẽ gọi method này.
     */
    @Transactional
    public void syncAllActiveOrders() {
        long total = ghnOrderRepo.count();
        log.info("👉 Tổng GHN order trong DB = {}", total);

        List<GhnOrder> all = ghnOrderRepo.findAll();
        all.forEach(o -> log.info(" - {} | status={} (enum={})",
                o.getOrderGhn(), o.getStatus().ordinal(), o.getStatus()));

        List<GhnOrder> ghnOrders = ghnOrderRepo.findAllByStatusIn(ACTIVE_STATUSES);
        if (ghnOrders.isEmpty()) {
            log.info("🔍 [GHN Sync] Không có GHN order active nào cần sync.");
            return;
        }

        log.info("🔍 [GHN Sync] Bắt đầu sync {} GHN orders", ghnOrders.size());

        for (GhnOrder ghnOrder : ghnOrders) {
            try {
                syncSingleOrder(ghnOrder);
            } catch (Exception e) {
                log.error("❌ [GHN Sync] Lỗi khi sync order_ghn={} : {}",
                        ghnOrder.getOrderGhn(), e.getMessage(), e);
            }
        }
    }

    /**
     * Đồng bộ 1 GHN order duy nhất
     */
    private void syncSingleOrder(GhnOrder ghnOrder) throws JsonProcessingException {
        String orderCode = ghnOrder.getOrderGhn();
        log.info("➡ [GHN Sync] Gọi order-detail cho GHN orderCode={}", orderCode);

        String bodyJson = objectMapper.writeValueAsString(
                java.util.Map.of("order_code", orderCode)
        );

        HttpEntity<String> entity = new HttpEntity<>(bodyJson, createHeaders());

        ResponseEntity<GhnOrderDetailWrapper> response = restTemplate.exchange(
                BASE_URL + "/v2/shipping-order/detail",
                HttpMethod.POST,
                entity,
                GhnOrderDetailWrapper.class
        );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || response.getBody().getData() == null) {
            log.warn("⚠ [GHN Sync] orderCode={} trả về lỗi: httpStatus={}, body={}",
                    orderCode, response.getStatusCode(), response.getBody());
            return;
        }

        GhnOrderDetailWrapper wrapper = response.getBody();
        if (wrapper.getCode() != 200) {
            log.warn("⚠ [GHN Sync] orderCode={} code={} message={}",
                    orderCode, wrapper.getCode(), wrapper.getMessage());
            return;
        }

        GhnOrderDetail detail = wrapper.getData();
        String ghnStatusRaw = detail.getStatus();
        GhnStatus newStatus = GhnStatus.fromGhnCode(ghnStatusRaw);

        if (newStatus == null) {
            log.warn("⚠ [GHN Sync] orderCode={} status={} không map được vào enum GhnStatus",
                    orderCode, ghnStatusRaw);
            return;
        }

//        if (ghnOrder.getStatus() == GhnStatus.PICKED && newStatus != GhnStatus.PICKED) {
//            log.info("⛔ [GHN Sync] GHN orderCode={} đang PICKED trong DB → bỏ qua, không update sang {}",
//                    orderCode, newStatus);
//            return;
//        }
        // 1️⃣ Cập nhật GhnOrder
        updateGhnOrderEntity(ghnOrder, detail, newStatus);

        // 2️⃣ Cập nhật StoreOrder + CustomerOrder
        updateStoreAndCustomerOrder(ghnOrder, detail, newStatus);
    }

    private void updateGhnOrderEntity(GhnOrder ghnOrder,
                                      GhnOrderDetail detail,
                                      GhnStatus newStatus) {
//        if (ghnOrder.getStatus() == GhnStatus.PICKED && newStatus != GhnStatus.PICKED) {
//            log.info("⛔ [GHN Sync] GHN order {} đang PICKED trong DB → không update sang {}",
//                    ghnOrder.getOrderGhn(), newStatus);
//            return;
//        }

        ghnOrder.setStatus(newStatus);

        // leadtime (string ISO) → LocalDateTime
        LocalDateTime expectedDelivery = parseOffsetDateTime(detail.getLeadtime());
        if (expectedDelivery != null) {
            ghnOrder.setExpectedDeliveryTime(expectedDelivery);
        }

        ghnOrderRepo.save(ghnOrder);
        log.info("✅ [GHN Sync] Cập nhật GhnOrder id={} code={} status={}",
                ghnOrder.getId(), ghnOrder.getOrderGhn(), newStatus);
    }

    private LocalDateTime parseOffsetDateTime(String iso) {
        if (iso == null) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            log.warn("⚠ Không parse được datetime: {}", iso);
            return null;
        }
    }

    /**
     * Map trạng thái GHN sang OrderStatus trong hệ thống
     */
    private OrderStatus mapToOrderStatus(GhnStatus ghnStatus) {
        return switch (ghnStatus) {
            case READY_TO_PICK, PICKING, MONEY_COLLECT_PICKING, PICKED,
                 STORING, TRANSPORTING, SORTING, DELIVERING, MONEY_COLLECT_DELIVERING ->
                    OrderStatus.SHIPPING;              // ông có thể đổi sang CONFIRMED / SHIPPING tùy enum

            case DELIVERED -> OrderStatus.DELIVERY_SUCCESS;

            case DELIVERY_FAIL,
                 WAITING_TO_RETURN,
                 RETURN, RETURN_TRANSPORTING, RETURN_SORTING, RETURNING, RETURN_FAIL ->
                    OrderStatus.DELIVERY_FAIL;        // hoặc RETURNING / RETURNED tuỳ ông định nghĩa

            case CANCEL -> OrderStatus.CANCELLED;

            case RETURNED -> OrderStatus.RETURNED;     // nếu enum có

            case EXCEPTION, DAMAGE, LOST -> OrderStatus.EXCEPTION; // nếu có

            default -> null;
        };
    }

    /**
     * ✅ NEW: cập nhật phí ship thật + chênh lệch cho StoreOrder
     * Gọi khi GHN chuyển sang trạng thái đang ship (PICKED)
     * Phí ship thật lấy từ GhnOrder.totalFee
     */
    private void updateActualShippingFeeForStoreOrder(StoreOrder storeOrder,
                                                      GhnOrder ghnOrder) {
        if (storeOrder == null || ghnOrder == null) return;

        BigDecimal actualFee = ghnOrder.getTotalFee();
        if (actualFee == null) {
            log.warn("⚠ [GHN Sync] GhnOrder {} không có totalFee (StoreOrder={})",
                    ghnOrder.getId(), storeOrder.getId());
            return;
        }

        BigDecimal estimated = storeOrder.getShippingFee() != null
                ? storeOrder.getShippingFee()
                : BigDecimal.ZERO;

        // Lưu phí ship GHN thực tế
        storeOrder.setActualShippingFee(actualFee);

        // Chênh lệch: GHN thực tế - khách đã trả
        BigDecimal diff = actualFee.subtract(estimated);
        storeOrder.setShippingExtraForStore(diff);

        log.info("🚚 [GHN Sync] StoreOrder {} - shippingFee(est)={} | actualShippingFee={} | diff={}",
                storeOrder.getId(), estimated, actualFee, diff);

        // ⚠ Không đổi grandTotal khách phải trả
        // grandTotal vẫn dùng shippingFee (estimate) trong @PrePersist/@PreUpdate của StoreOrder.
        // diff sẽ dùng cho settlement / ví sau này.
    }


    private void updateStoreAndCustomerOrder(GhnOrder ghnOrder,
                                             GhnOrderDetail detail,
                                             GhnStatus newGhnStatus) {
        StoreOrder storeOrder = storeOrderRepo.findById(ghnOrder.getStoreOrderId())
                .orElse(null);
        if (storeOrder == null) {
            log.warn("⚠ [GHN Sync] Không tìm thấy StoreOrder id={} cho GHN order {}",
                    ghnOrder.getStoreOrderId(), ghnOrder.getOrderGhn());
            return;
        }

        OrderStatus mappedStatus = mapToOrderStatus(newGhnStatus);

        // Nếu mappedStatus null (không map được) thì thôi
        if (mappedStatus == null) {
            log.warn("⚠ [GHN Sync] Không map được OrderStatus từ GhnStatus={} (StoreOrder={})",
                    newGhnStatus, storeOrder.getId());
            return;
        }

        storeOrder.setStatus(mappedStatus);

        // 🔥 Khi GHN sang trạng thái đang ship → cập nhật phí ship thật
        if (newGhnStatus == GhnStatus.PICKED) {
            updateActualShippingFeeForStoreOrder(storeOrder, ghnOrder);
            updateReturnShippingFeeWhenPicked(ghnOrder);
        }

        // ✅ Khi GHN đã DELIVERED → set deliveredAt cho StoreOrder (lần đầu)
        if (newGhnStatus == GhnStatus.DELIVERED) {
            LocalDateTime finish = parseOffsetDateTime(detail.getFinish_date());
            if (finish == null) {
                finish = LocalDateTime.now();
            }

            // chỉ set nếu chưa có (tránh override nếu đã set tay ở chỗ khác)
            if (storeOrder.getDeliveredAt() == null) {
                storeOrder.setDeliveredAt(finish);
            }
        }

        storeOrderRepo.save(storeOrder);
        log.info("✅ [GHN Sync] Cập nhật StoreOrder {} → status={} deliveredAt={}",
                storeOrder.getId(), storeOrder.getStatus(), storeOrder.getDeliveredAt());
        // ==== Cập nhật nợ cho StoreOrder ====
        try {
            storeOrderDebtCron.recalcDebtAndWalletForOrder(storeOrder.getId(), mappedStatus);
        } catch (Exception e) {
            log.error("❌ [GHN Sync] recalc debt failed for storeOrderId={} : {}",
                    storeOrder.getId(), e.getMessage(), e);
        }

        // ==== Cập nhật CustomerOrder ====
        CustomerOrder customerOrder = storeOrder.getCustomerOrder();
        if (customerOrder == null) {
            log.warn("⚠ StoreOrder {} không có customerOrder", storeOrder.getId());
            return;
        }

        // Lấy toàn bộ storeOrders của customerOrder để gom status
        List<StoreOrder> allStoreOrders =
                storeOrderRepo.findAllByCustomerOrder(customerOrder);

        boolean allDelivered = allStoreOrders.stream()
                .allMatch(so -> so.getStatus() == OrderStatus.DELIVERY_SUCCESS);

        if (allDelivered) {
            // CustomerOrder coi như giao xong toàn bộ
            customerOrder.setStatus(OrderStatus.DELIVERY_SUCCESS);

            // deliveredAt = max deliveredAt trong các storeOrder
            LocalDateTime maxDelivered =
                    allStoreOrders.stream()
                            .map(StoreOrder::getDeliveredAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(LocalDateTime.now());

            // cũng chỉ set nếu chưa tồn tại, để giữ "lần đầu giao xong"
            if (customerOrder.getDeliveredAt() == null) {
                customerOrder.setDeliveredAt(maxDelivered);
            }

            customerOrderRepo.save(customerOrder);

            log.info("🎉 [GHN Sync] CustomerOrder {} đã DELIVERY_SUCCESS (deliveredAt={})",
                    customerOrder.getId(), customerOrder.getDeliveredAt());

            if (customerOrder.getPaymentMethod() == PaymentMethod.COD) {
                try {
                    settlementService.recordCodDeliverySuccess(customerOrder);
                } catch (Exception e) {
                    log.error("❌ [GHN Sync] Lỗi khi record COD settlement cho order {}: {}",
                            customerOrder.getId(), e.getMessage(), e);
                }
            }
        } else {
            // Nếu chưa giao hết: có thể set trạng thái “SHIPPING” (nếu hiện tại chưa phải CANCEL/UNPAID)
            if (customerOrder.getStatus() != OrderStatus.CANCELLED
                    && customerOrder.getStatus() != OrderStatus.UNPAID) {
                customerOrder.setStatus(OrderStatus.SHIPPING);
                customerOrderRepo.save(customerOrder);
                log.info("ℹ [GHN Sync] CustomerOrder {} → SHIPPING (chưa giao hết store)",
                        customerOrder.getId());
            }
        }
    }

    /**
     * ✅ NEW: Khi GHN đơn RETURN được shipper PICKED
     *  - Cập nhật shippingFee = totalFee (phí thật GHN)
     *  - Đánh dấu picked = true để cron job khác xử lý trừ tiền sau
     */
    private void updateReturnShippingFeeWhenPicked(GhnOrder ghnOrder) {
        String orderCode = ghnOrder.getOrderGhn();
        if (orderCode == null || orderCode.isBlank()) return;

        returnShippingFeeRepo.findByGhnOrderCode(orderCode).ifPresent(feeLog -> {
            // nếu đã marked picked rồi thì khỏi làm lại
            if (feeLog.isPicked()) {
                log.info("[RETURN FEE] ghnOrderCode={} đã picked trước đó, bỏ qua", orderCode);
                return;
            }

            BigDecimal actualFee = ghnOrder.getTotalFee();
            if (actualFee != null && actualFee.compareTo(BigDecimal.ZERO) > 0) {
                // nếu muốn dùng phí ship thật GHN thì set lại shippingFee
                feeLog.setShippingFee(actualFee);
            }

            feeLog.setPicked(true);
            returnShippingFeeRepo.save(feeLog);

            log.info("🚚 [RETURN FEE] Mark picked=true for returnRequestId={} | ghnOrderCode={} | shippingFee={}",
                    feeLog.getReturnRequestId(), orderCode, feeLog.getShippingFee());
        });
    }

}
