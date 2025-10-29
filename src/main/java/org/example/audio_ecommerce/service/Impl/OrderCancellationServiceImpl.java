package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final CustomerOrderRepository customerOrderRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final StoreOrderCancellationRepository cancelRepo;
    private final SettlementService settlementService;

    /** KH hủy toàn bộ nếu CustomerOrder còn PENDING => refund ngay về ví KH, không cần shop duyệt */
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

        // Refund toàn bộ (nếu là online đã vào Platform pending)
        settlementService.refundEntireOrderToCustomerWallet(order);

        // Set tất cả StoreOrder -> CANCELLED
        var storeOrders = storeOrderRepo.findAllByCustomerOrder_Id(order.getId());
        for (StoreOrder so : storeOrders) {
            so.setStatus(OrderStatus.CANCELLED);
        }
        storeOrderRepo.saveAll(storeOrders);

        // CustomerOrder -> CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        customerOrderRepo.save(order);

        // Optionally: log reason/note ở một bảng riêng (omitted)
        return BaseResponse.success("Order cancelled & refunded to wallet");
    }

    /** Shop duyệt hủy: hoàn phần tiền của storeOrder về ví KH, set storeOrder=CANCELLED.
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

        // Phải đang AWAITING_SHIPMENT mới có case shop duyệt
        if (storeOrder.getStatus() != OrderStatus.AWAITING_SHIPMENT) {
            return BaseResponse.error("StoreOrder is not in AWAITING_SHIPMENT");
        }

        // Lấy request gần nhất ở trạng thái REQUESTED (nếu có)
        var requests = cancelRepo.findAllByStoreOrder_Id(storeOrderId);
        var req = requests.stream().filter(r -> r.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second).orElse(null);
        if (req != null) {
            req.setStatus(CancellationRequestStatus.APPROVED);
            req.setProcessedAt(LocalDateTime.now());
            cancelRepo.save(req);
        }

        // 1) Refund phần tiền của storeOrder về ví KH, reverse pending của ví shop & platform
        settlementService.refundStorePartToCustomerWallet(storeOrder);

        // 2) Đánh dấu storeOrder CANCELLED
        storeOrder.setStatus(OrderStatus.CANCELLED);
        storeOrderRepo.save(storeOrder);

        // 3) Nếu tất cả StoreOrder của CustomerOrder đều CANCELLED -> CustomerOrder CANCELLED
        CustomerOrder customerOrder = storeOrder.getCustomerOrder();
        boolean allCancelled = storeOrderRepo.findAllByCustomerOrder_Id(customerOrder.getId())
                .stream().allMatch(so -> so.getStatus() == OrderStatus.CANCELLED);
        if (allCancelled) {
            customerOrder.setStatus(OrderStatus.CANCELLED);
            customerOrderRepo.save(customerOrder);
        }

        return BaseResponse.success("Cancellation approved & refunded to wallet");
    }

    /** Shop từ chối hủy: giữ nguyên tiền/settlement */
    @Override
    @Transactional
    public BaseResponse<Void> shopRejectCancel(UUID storeId, UUID storeOrderId, String note) {
        StoreOrder storeOrder = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));
        if (!storeOrder.getStore().getStoreId().equals(storeId)) {
            return BaseResponse.error("Store does not own this order");
        }

        var requests = cancelRepo.findAllByStoreOrder_Id(storeOrderId);
        var req = requests.stream().filter(r -> r.getStatus() == CancellationRequestStatus.REQUESTED)
                .reduce((first, second) -> second).orElse(null);
        if (req == null) {
            return BaseResponse.error("No pending cancellation request");
        }

        req.setStatus(CancellationRequestStatus.REJECTED);
        req.setProcessedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            req.setNote((req.getNote() == null ? "" : req.getNote() + " | ") + "[REJECT] " + note);
        }
        cancelRepo.save(req);

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

        // Tạo yêu cầu hủy
        StoreOrderCancellationRequest req = StoreOrderCancellationRequest.builder()
                .storeOrder(target)
                .reason(reason)
                .note(note)
                .status(CancellationRequestStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
        cancelRepo.save(req);

        return BaseResponse.success("Cancellation request sent to shop for approval");
    }

}
