package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.NotificationCreatorService;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final CustomerOrderRepository customerOrderRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final StoreOrderCancellationRepository cancelRepo;
    private final CustomerOrderCancellationRepository customerCancelRepo;
    private final SettlementService settlementService;
    private final ProductRepository productRepo;
    private final ProductVariantRepository productVariantRepo;
    private final NotificationCreatorService notificationCreatorService;

    /**
     * KH hủy toàn bộ nếu CustomerOrder còn PENDING => refund ngay về ví KH, không cần shop duyệt
     */
    @Override
    @Transactional
    public BaseResponse<Void> customerCancelWholeOrderIfPending(UUID customerId, UUID customerOrderId,
                                                                CancellationReason reason, String note) {
        CustomerOrder order = customerOrderRepo.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));
        if (!order.getCustomer().getId().equals(customerId)) {
            return BaseResponse.error("Customer does not own this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return BaseResponse.error("Order status must be PENDING to cancel immediately");
        }

        // ✅ Log vào bảng customer_order_cancellation (auto APPROVED)
        LocalDateTime now = LocalDateTime.now();
        CustomerOrderCancellationRequest coCancel = CustomerOrderCancellationRequest.builder()
                .customerOrder(order)
                .reason(reason)
                .note(note)
                .status(CancellationRequestStatus.APPROVED)
                .requestedAt(now)
                .processedAt(now)
                .build();
        customerCancelRepo.save(coCancel);

        // Refund toàn bộ (nếu là online đã vào Platform pending)
        settlementService.refundEntireOrderToCustomerWallet(order);

        // Set tất cả StoreOrder -> CANCELLED
        var storeOrders = storeOrderRepo.findAllByCustomerOrder_Id(order.getId());
        for (StoreOrder so : storeOrders) {
            restockProductsForStoreOrder(so);
            so.setStatus(OrderStatus.CANCELLED);
        }
        storeOrderRepo.saveAll(storeOrders);

        // CustomerOrder -> CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        customerOrderRepo.save(order);

        // CUSTOMER
        notificationCreatorService.createAndSend(
                NotificationTarget.CUSTOMER,
                order.getCustomer().getId(),
                NotificationType.ORDER_CANCELLED,
                "Đơn hàng " + order.getOrderCode() + " đã được huỷ",
                buildCustomerCancelMessage(order, reason, note),
                "/customer/orders/" + order.getId(),
                "{\"customerOrderId\":\"" + order.getId() + "\"}",
                Map.of("screen", "ORDER_DETAIL")
        );

        // STORE
        for (StoreOrder so : storeOrders) {
            Store store = so.getStore();
            if (store == null) continue;

            notificationCreatorService.createAndSend(
                    NotificationTarget.STORE,
                    store.getStoreId(),
                    NotificationType.ORDER_CANCELLED,
                    "Đơn hàng " + order.getOrderCode() + " đã bị khách huỷ",
                    buildStoreCancelMessage(order, reason, note),
                    "/seller/orders/" + so.getId(),
                    "{\"storeOrderId\":\"" + so.getId() + "\",\"customerOrderId\":\"" + order.getId() + "\"}",
                    Map.of("screen", "SELLER_ORDER_DETAIL")
            );
        }


        // Optionally: log reason/note ở một bảng riêng (omitted)
        return BaseResponse.success("Order cancelled & refunded to wallet");
    }

    /**
     * Shop duyệt hủy: hoàn phần tiền của storeOrder về ví KH, set storeOrder=CANCELLED.
     * Nếu tất cả storeOrder đều CANCELLED => CustomerOrder cũng CANCELLED.
     */
    @Override
    @Transactional
    public BaseResponse<Void> shopApproveCancel(UUID storeId, UUID storeOrderId) {
        StoreOrder storeOrder = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));

        if (!storeOrder.getStore().getStoreId().equals(storeId)) {
            return BaseResponse.error("Store does not own this order");
        }

        if (storeOrder.getStatus() != OrderStatus.AWAITING_SHIPMENT) {
            return BaseResponse.error("StoreOrder is not in AWAITING_SHIPMENT");
        }

        LocalDateTime now = LocalDateTime.now();

        // ===== 1) Cập nhật StoreOrderCancellationRequest về APPROVED =====
        var requests = cancelRepo.findAllByStoreOrder_Id(storeOrderId);
        var req = requests.stream()
                .filter(r -> r.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second)
                .orElse(null);
        if (req != null) {
            req.setStatus(CancellationRequestStatus.APPROVED);
            req.setProcessedAt(now);
            cancelRepo.save(req);
        }

        // ===== 2) Cập nhật CustomerOrderCancellationRequest tương ứng về APPROVED =====
        CustomerOrder customerOrder = storeOrder.getCustomerOrder();
        var customerCancels = customerCancelRepo.findAllByCustomerOrder_Id(customerOrder.getId());
        var coReq = customerCancels.stream()
                .filter(c -> c.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second)
                .orElse(null);
        if (coReq != null) {
            coReq.setStatus(CancellationRequestStatus.APPROVED);
            coReq.setProcessedAt(now);
            customerCancelRepo.save(coReq);
        }

        // ===== 3) Refund phần tiền của storeOrder về ví KH =====
        settlementService.refundStorePartToCustomerWallet(storeOrder);

        // ✅ 3b) Cộng lại stock cho product/variant tương ứng
        restockProductsForStoreOrder(storeOrder);

        // 4) Đánh dấu storeOrder CANCELLED
        storeOrder.setStatus(OrderStatus.CANCELLED);
        storeOrderRepo.save(storeOrder);

        // 5) Nếu tất cả StoreOrder của CustomerOrder đều CANCELLED -> CustomerOrder CANCELLED
        boolean allCancelled = storeOrderRepo.findAllByCustomerOrder_Id(customerOrder.getId())
                .stream().allMatch(so -> so.getStatus() == OrderStatus.CANCELLED);
        if (allCancelled) {
            customerOrder.setStatus(OrderStatus.CANCELLED);
            customerOrderRepo.save(customerOrder);
        }

        // ================== 🔔 NOTIFICATION ==================

        // CUSTOMER: thông báo yêu cầu huỷ đã được shop chấp nhận
        notificationCreatorService.createAndSend(
                NotificationTarget.CUSTOMER,
                customerOrder.getCustomer().getId(),
                NotificationType.ORDER_CANCEL_APPROVED, // gợi ý enum
                "Yêu cầu huỷ đơn " + customerOrder.getOrderCode() + " đã được chấp nhận",
                "Cửa hàng đã chấp nhận yêu cầu huỷ. Số tiền tương ứng sẽ được hoàn về ví của bạn.",
                "/customer/orders/" + customerOrder.getId(),
                "{\"customerOrderId\":\"" + customerOrder.getId() + "\",\"storeOrderId\":\"" + storeOrder.getId() + "\"}",
                Map.of(
                        "screen", "ORDER_DETAIL",
                        "customerOrderId", String.valueOf(customerOrder.getId()),
                        "storeOrderId", String.valueOf(storeOrder.getId())
                )
        );

        // STORE: thông báo đã duyệt huỷ thành công
        notificationCreatorService.createAndSend(
                NotificationTarget.STORE,
                storeOrder.getStore().getStoreId(),
                NotificationType.ORDER_CANCEL_APPROVED,
                "Đã duyệt huỷ đơn " + customerOrder.getOrderCode(),
                "Bạn đã chấp nhận yêu cầu huỷ đơn hàng. Hệ thống đã xử lý hoàn tiền cho khách.",
                "/seller/orders/" + storeOrder.getId(),
                "{\"storeOrderId\":\"" + storeOrder.getId() + "\",\"customerOrderId\":\"" + customerOrder.getId() + "\"}",
                Map.of(
                        "screen", "SELLER_ORDER_DETAIL",
                        "storeOrderId", String.valueOf(storeOrder.getId()),
                        "customerOrderId", String.valueOf(customerOrder.getId())
                )
        );

        return BaseResponse.success("Cancellation approved & refunded to wallet");
    }


    /**
     * Shop từ chối hủy: giữ nguyên tiền/settlement
     */
    @Override
    @Transactional
    public BaseResponse<Void> shopRejectCancel(UUID storeId, UUID storeOrderId, String note) {
        StoreOrder storeOrder = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));
        if (!storeOrder.getStore().getStoreId().equals(storeId)) {
            return BaseResponse.error("Store does not own this order");
        }

        LocalDateTime now = LocalDateTime.now();

        // ===== 1) Lấy request huỷ phía store-order (bắt buộc phải có) =====
        var requests = cancelRepo.findAllByStoreOrder_Id(storeOrderId);
        var req = requests.stream()
                .filter(r -> r.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second)
                .orElse(null);
        if (req == null) {
            return BaseResponse.error("No pending cancellation request");
        }

        req.setStatus(CancellationRequestStatus.REJECTED);
        req.setProcessedAt(now);
        if (note != null && !note.isBlank()) {
            req.setNote((req.getNote() == null ? "" : req.getNote() + " | ") + "[REJECT] " + note);
        }
        cancelRepo.save(req);

        // ===== 2) Cập nhật CustomerOrderCancellationRequest tương ứng về REJECTED =====
        CustomerOrder customerOrder = storeOrder.getCustomerOrder();
        var customerCancels = customerCancelRepo.findAllByCustomerOrder_Id(customerOrder.getId());
        var coReq = customerCancels.stream()
                .filter(c -> c.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second)
                .orElse(null);
        if (coReq != null) {
            coReq.setStatus(CancellationRequestStatus.REJECTED);
            coReq.setProcessedAt(now);
            if (note != null && !note.isBlank()) {
                coReq.setNote((coReq.getNote() == null ? "" : coReq.getNote() + " | ") + "[SHOP_REJECT] " + note);
            }
            customerCancelRepo.save(coReq);
        }

        // ================== 🔔 NOTIFICATION ==================

        // CUSTOMER: yêu cầu huỷ bị từ chối
        String customerMsg = "Cửa hàng đã từ chối yêu cầu huỷ đơn " + customerOrder.getOrderCode() + ".";
        if (note != null && !note.isBlank()) {
            customerMsg += " Lý do: " + note;
        }

        notificationCreatorService.createAndSend(
                NotificationTarget.CUSTOMER,
                customerOrder.getCustomer().getId(),
                NotificationType.ORDER_CANCEL_REJECTED,
                "Yêu cầu huỷ đơn " + customerOrder.getOrderCode() + " bị từ chối",
                customerMsg,
                "/customer/orders/" + customerOrder.getId(),
                "{\"customerOrderId\":\"" + customerOrder.getId() + "\",\"storeOrderId\":\"" + storeOrder.getId() + "\"}",
                Map.of(
                        "screen", "ORDER_DETAIL",
                        "customerOrderId", String.valueOf(customerOrder.getId()),
                        "storeOrderId", String.valueOf(storeOrder.getId())
                )
        );

        // STORE: optional – thông báo để multi-device cập nhật
        notificationCreatorService.createAndSend(
                NotificationTarget.STORE,
                storeOrder.getStore().getStoreId(),
                NotificationType.ORDER_CANCEL_REJECTED,
                "Đã từ chối yêu cầu huỷ đơn " + customerOrder.getOrderCode(),
                "Bạn đã từ chối yêu cầu huỷ đơn từ khách hàng.",
                "/seller/orders/" + storeOrder.getId(),
                "{\"storeOrderId\":\"" + storeOrder.getId() + "\",\"customerOrderId\":\"" + customerOrder.getId() + "\"}",
                Map.of(
                        "screen", "SELLER_ORDER_DETAIL",
                        "storeOrderId", String.valueOf(storeOrder.getId()),
                        "customerOrderId", String.valueOf(customerOrder.getId())
                )
        );

        // Không đụng tới tiền/settlement
        return BaseResponse.success("Cancellation request rejected");
    }


    @Override
    @Transactional
    public BaseResponse<Void> customerRequestCancelStoreOrderByCustomerOrderId(
            UUID customerId, UUID customerOrderId, CancellationReason reason, String note) {

        CustomerOrder co = customerOrderRepo.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        if (!co.getCustomer().getId().equals(customerId)) {
            return BaseResponse.error("Customer does not own this order");
        }

        // Lấy tất cả store-order của customerOrder
        var storeOrders = storeOrderRepo.findAllByCustomerOrder_Id(customerOrderId);

        // 🔒 Giả định kiến trúc: mỗi CustomerOrder chỉ có 1 StoreOrder
        if (storeOrders == null || storeOrders.isEmpty()) {
            return BaseResponse.error("No store order found for this customer order");
        }
        if (storeOrders.size() != 1) {
            // Nếu về sau có case >1 (không mong muốn), fail an toàn để tránh hủy nhầm
            return BaseResponse.error("Ambiguous store orders for this customer order");
        }

        StoreOrder target = storeOrders.get(0);

        // Chỉ cho phép request khi đang AWAITING_SHIPMENT
        if (target.getStatus() != OrderStatus.AWAITING_SHIPMENT) {
            return BaseResponse.error("StoreOrder must be AWAITING_SHIPMENT to request cancel");
        }

        LocalDateTime now = LocalDateTime.now();

        // ✅ Log vào bảng customer_order_cancellation (REQUESTED)
        CustomerOrderCancellationRequest coCancel = CustomerOrderCancellationRequest.builder()
                .customerOrder(co)
                .reason(reason)
                .note(note)
                .status(CancellationRequestStatus.REQUESTED)
                .requestedAt(now)
                .build();
        customerCancelRepo.save(coCancel);

        // Tạo yêu cầu hủy
        StoreOrderCancellationRequest req = StoreOrderCancellationRequest.builder()
                .storeOrder(target)
                .reason(reason)
                .note(note)
                .status(CancellationRequestStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
        cancelRepo.save(req);

        // ================== 🔔 NOTIFICATION ==================

        // Thông báo cho STORE: có yêu cầu huỷ cần duyệt
        String storeMsg = buildStoreApproveNeededMessage(co, reason, note);

        notificationCreatorService.createAndSend(
                NotificationTarget.STORE,
                target.getStore().getStoreId(),
                NotificationType.ORDER_CANCEL_REQUESTED,
                "Yêu cầu huỷ đơn " + co.getOrderCode(),
                storeMsg,
                "/seller/orders/" + target.getId(),  // màn duyệt huỷ
                "{\"storeOrderId\":\"" + target.getId() + "\",\"customerOrderId\":\"" + co.getId() + "\"}",
                Map.of(
                        "screen", "SELLER_ORDER_DETAIL",
                        "storeOrderId", String.valueOf(target.getId()),
                        "customerOrderId", String.valueOf(co.getId())
                )
        );

        return BaseResponse.success("Cancellation request sent to shop for approval");
    }

    // ========================================================================
    // ✅ NEW: Customer xem các request hủy liên quan tới 1 CustomerOrder
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<StoreOrderCancellationRequest> getCustomerCancellationRequests(
            UUID customerId,
            UUID customerOrderId
    ) {
        CustomerOrder co = customerOrderRepo.findById(customerOrderId)
                .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));

        if (!co.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Customer does not own this order");
        }

        // Lấy tất cả store-order thuộc customer-order này
        var storeOrders = storeOrderRepo.findAllByCustomerOrder_Id(customerOrderId);
        if (storeOrders == null || storeOrders.isEmpty()) {
            return java.util.List.of();
        }

        // Gom tất cả cancellation request của mọi store-order
        java.util.List<StoreOrderCancellationRequest> result = new java.util.ArrayList<>();
        for (StoreOrder so : storeOrders) {
            var requests = cancelRepo.findAllByStoreOrder_Id(so.getId());
            if (requests != null && !requests.isEmpty()) {
                result.addAll(requests);
            }
        }
        return result;
    }

    // ========================================================================
    // ✅ NEW: Store xem các request hủy của 1 StoreOrder cụ thể
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<StoreOrderCancellationRequest> getStoreCancellationRequests(
            UUID storeId,
            UUID storeOrderId
    ) {
        StoreOrder storeOrder = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));

        if (!storeOrder.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Store does not own this order");
        }

        return cancelRepo.findAllByStoreOrder_Id(storeOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderCancellationRequest> getAllCustomerOrderCancellations(UUID customerId) {
        return customerCancelRepo.findAllByCustomerOrder_Customer_Id(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreOrderCancellationRequest> getAllStoreCancellationRequests(UUID storeId) {
        // Cách 1: dùng repo store-order
        var storeOrders = storeOrderRepo.findAllByStore_StoreId(storeId);
        if (storeOrders == null || storeOrders.isEmpty()) return List.of();

        java.util.List<StoreOrderCancellationRequest> result = new java.util.ArrayList<>();
        for (StoreOrder so : storeOrders) {
            var requests = cancelRepo.findAllByStoreOrder_Id(so.getId());
            if (requests != null && !requests.isEmpty()) {
                result.addAll(requests);
            }
        }
        return result;
    }

    /**
     * Cộng lại tồn kho cho các item PRODUCT trong 1 StoreOrder khi huỷ.
     * - Nếu StoreOrderItem có variantId -> +qty vào variant.variantStock và product.stockQuantity
     * - Nếu không có variantId -> +qty vào product.stockQuantity
     * COMBO hiện không xử lý stock (có thể bổ sung sau).
     */
    private void restockProductsForStoreOrder(StoreOrder storeOrder) {
        if (storeOrder == null || storeOrder.getItems() == null) return;

        for (StoreOrderItem item : storeOrder.getItems()) {
            if (item == null) continue;

            // Chỉ xử lý type PRODUCT
            if (!"PRODUCT".equalsIgnoreCase(item.getType())) {
                continue;
            }

            int qty = item.getQuantity();
            if (qty <= 0) continue;

            // 1) Cộng lại stock cho variant nếu có
            if (item.getVariantId() != null) {
                productVariantRepo.findById(item.getVariantId()).ifPresent(variant -> {
                    Integer vs = variant.getVariantStock();
                    if (vs == null) vs = 0;
                    variant.setVariantStock(vs + qty);
                });
            }

            // 2) Cộng lại stock cho product (refId là productId)
            if (item.getRefId() != null) {
                productRepo.findById(item.getRefId()).ifPresent(product -> {
                    Integer ps = product.getStockQuantity();
                    if (ps == null) ps = 0;
                    product.setStockQuantity(ps + qty);
                });
            }
        }
    }

    private String buildCustomerCancelMessage(CustomerOrder order,
                                              CancellationReason reason,
                                              String note) {
        StringBuilder sb = new StringBuilder("Đơn hàng của bạn đã được huỷ thành công.");
        if (reason != null) {
            sb.append(" Lý do: ").append(reason.name());
        }
        if (note != null && !note.isBlank()) {
            sb.append(" Ghi chú: ").append(note);
        }
        return sb.toString();
    }

    private String buildStoreCancelMessage(CustomerOrder order,
                                           CancellationReason reason,
                                           String note) {
        StringBuilder sb = new StringBuilder("Khách hàng đã huỷ đơn hàng trước khi xử lý giao.");
        if (reason != null) {
            sb.append(" Lý do: ").append(reason.name());
        }
        if (note != null && !note.isBlank()) {
            sb.append(" Ghi chú: ").append(note);
        }
        return sb.toString();
    }

    private String buildStoreApproveNeededMessage(CustomerOrder co,
                                                  CancellationReason reason,
                                                  String note) {
        StringBuilder sb = new StringBuilder("Khách hàng đã yêu cầu huỷ đơn hàng, vui lòng xem xét duyệt.");
        if (reason != null) {
            sb.append(" Lý do: ").append(reason.name());
        }
        if (note != null && !note.isBlank()) {
            sb.append(" Ghi chú: ").append(note);
        }
        return sb.toString();
    }

}
