package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.DeliveryAssignmentResponse;
import org.example.audio_ecommerce.dto.response.OrderItemResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.DeliveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;            // ✔ Spring Data
import org.springframework.data.domain.Sort;                // ✔ Spring Data
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {
    private final StoreOrderItemRepository storeOrderItemRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final StoreRepository storeRepo;
    private final StaffRepository staffRepo;
    private final DeliveryAssignmentRepository assignmentRepo;
    private final DeliveryLocationLogRepository locationRepo;
    private final DeliveryProofRepository proofRepo;
    private final CustomerOrderRepository customerOrderRepository;

    // ========= Helpers =========

    private StoreOrder mustGetStoreOrderOfStore(UUID storeId, UUID storeOrderId) {
        StoreOrder so = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));
        if (!so.getStore().getStoreId().equals(storeId)) {
            throw new SecurityException("StoreOrder not belong to this store");
        }
        return so;
    }

    // ========= Commands =========

    @Override
    @Transactional
    public void assignDeliveryStaff(UUID storeId, UUID storeOrderId, UUID deliveryStaffId, UUID preparedByStaffId, String note) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        Staff deliver = staffRepo.findById(deliveryStaffId)
                .orElseThrow(() -> new NoSuchElementException("Delivery staff not found"));
        if (!deliver.getStore().getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Staff not in this store");
        }

        Staff prepared = null;
        if (preparedByStaffId != null) {
            prepared = staffRepo.findById(preparedByStaffId)
                    .orElseThrow(() -> new NoSuchElementException("PreparedBy staff not found"));
            if (!prepared.getStore().getStoreId().equals(storeId)) {
                throw new IllegalArgumentException("Prepared staff not in this store");
            }
        }

        DeliveryAssignment assignment = assignmentRepo.findByStoreOrder_Id(storeOrderId)
                .orElse(DeliveryAssignment.builder().storeOrder(so).build());

        assignment.setDeliveryStaff(deliver);
        assignment.setPreparedBy(prepared);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setNote(note);
        assignmentRepo.save(assignment);

        // chuyển trạng thái sang READY_FOR_PICKUP
        so.setStatus(OrderStatus.READY_FOR_PICKUP);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void markReadyForPickup(UUID storeId, UUID storeOrderId) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);
        so.setStatus(OrderStatus.READY_FOR_DELIVERY);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void markOutForDelivery(UUID storeId, UUID storeOrderId) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        DeliveryAssignment asg = assignmentRepo.findByStoreOrder_Id(storeOrderId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        asg.setPickUpAt(LocalDateTime.now());
        assignmentRepo.save(asg);

        so.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void markDeliveredWaitingConfirm(UUID storeId, UUID storeOrderId) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        DeliveryAssignment asg = assignmentRepo.findByStoreOrder_Id(storeOrderId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        asg.setDeliveredAt(LocalDateTime.now());
        assignmentRepo.save(asg);

        so.setStatus(OrderStatus.DELIVERED_WAITING_CONFIRM);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void confirmDeliverySuccess(UUID storeId, UUID storeOrderId, String photoUrl, boolean installed, String note) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        // proof
        Staff deliver = assignmentRepo.findByStoreOrder_Id(storeOrderId)
                .map(DeliveryAssignment::getDeliveryStaff)
                .orElse(null);

        DeliveryProof p = DeliveryProof.builder()
                .storeOrder(so)
                .deliveryStaff(deliver)
                .photoUrl(photoUrl)
                .installed(installed)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        proofRepo.save(p);

        // hoàn tất đơn
        so.setStatus(OrderStatus.DELIVERY_SUCCESS);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void markDeliveryDenied(UUID storeId, UUID storeOrderId, String reason) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        String note = Optional.ofNullable(so.getShipNote()).orElse("");
        note = (note.isBlank() ? "" : note + " | ") + "[DENY_RECEIVE] " + reason;
        so.setShipNote(note);

        so.setStatus(OrderStatus.DELIVERY_DENIED);
        storeOrderRepo.save(so);
        syncCustomerOrderStatus(so);
    }

    @Override
    @Transactional
    public void pushLocation(UUID storeId, UUID storeOrderId, Double lat, Double lng, Double speed, String addressText) {
        StoreOrder so = mustGetStoreOrderOfStore(storeId, storeOrderId);

        DeliveryAssignment asg = assignmentRepo.findByStoreOrder_Id(storeOrderId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        DeliveryLocationLog log = DeliveryLocationLog.builder()
                .storeOrder(so)
                .assignment(asg)
                .latitude(lat)
                .longitude(lng)
                .speedKmh(speed)
                .addressText(addressText)
                .loggedAt(LocalDateTime.now())
                .build();
        locationRepo.save(log);

        // auto chuyển OUT_FOR_DELIVERY nếu còn READY_FOR_PICKUP
        if (so.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            so.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            storeOrderRepo.save(so);
            syncCustomerOrderStatus(so);
        }
    }

    // ========= Queries =========

    @Override
    @Transactional
    public List<DeliveryAssignmentResponse> listAssignments(UUID storeId, OrderStatus status) {
        storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found"));

        List<DeliveryAssignment> entities = (status == null)
                ? assignmentRepo.findAllByStoreIdFetchItems(storeId)
                : assignmentRepo.findAllByStoreIdAndStatusFetchItems(storeId, status);
        return entities.stream().map(this::toDTO).toList();

    }

    @Override
    @Transactional
    public Page<DeliveryAssignmentResponse> pageAssignments(UUID storeId, OrderStatus status, int page, int size, String sort) {
        storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found"));

        Sort s = Sort.by((sort == null || sort.isBlank()) ? "assignedAt" : sort).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), s);

        // lấy Page<DeliveryAssignment> từ repo
        Page<DeliveryAssignment> p =
                assignmentRepo.findPageByStoreIdAndStatusFetchItems(storeId, status, pageable);
        return p.map(this::toDTO);

    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse getAssignment(UUID storeId, UUID assignmentId) {
        var a = assignmentRepo.findByIdFetchItems(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("Assignment not found"));
        if (a.getStoreOrder()==null || a.getStoreOrder().getStore()==null
                || !storeId.equals(a.getStoreOrder().getStore().getStoreId())) {
            throw new SecurityException("Assignment not belong to this store");
        }
        return toDTO(a);

    }

    @Override
    @Transactional
    public StoreOrder getStoreOrderEntity(UUID storeOrderId) {
        return storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found"));
    }

    // ========= Sync parent CustomerOrder =========

    private void syncCustomerOrderStatus(StoreOrder changed) {
        CustomerOrder co = changed.getCustomerOrder();
        if (co == null) return;

        var all = storeOrderRepo.findAllByCustomerOrder_Id(co.getId());
        boolean allSuccess = all.stream().allMatch(so -> so.getStatus() == OrderStatus.DELIVERY_SUCCESS);
        boolean anyDeliveredWaiting = all.stream().anyMatch(so -> so.getStatus() == OrderStatus.DELIVERED_WAITING_CONFIRM);
        boolean anyOutFor = all.stream().anyMatch(so -> so.getStatus() == OrderStatus.OUT_FOR_DELIVERY);
        boolean allReady = all.stream().allMatch(so -> so.getStatus() == OrderStatus.READY_FOR_PICKUP);
        boolean anyDenied = all.stream().anyMatch(so -> so.getStatus() == OrderStatus.DELIVERY_DENIED);

        OrderStatus newParent;
        if (allSuccess) newParent = OrderStatus.DELIVERY_SUCCESS;
        else if (anyDeliveredWaiting) newParent = OrderStatus.DELIVERED_WAITING_CONFIRM;
        else if (anyOutFor) newParent = OrderStatus.OUT_FOR_DELIVERY;
        else if (allReady) newParent = OrderStatus.READY_FOR_PICKUP;
        else newParent = co.getStatus() == null ? OrderStatus.PENDING : co.getStatus();

        if (anyDenied) {
            String note = Optional.ofNullable(co.getShipNote()).orElse("");
            if (!note.contains("[ANY_DENIED]")) {
                note = (note.isBlank() ? "" : note + " | ") + "[ANY_DENIED]";
                co.setShipNote(note);
            }
            // Nếu có chính sách riêng, có thể set newParent = OrderStatus.DELIVERY_DENIED;
        }

        if (co.getStatus() != newParent) {
            co.setStatus(newParent);
            customerOrderRepository.save(co);
        }
    }

    @Override
    @Transactional
    public List<DeliveryAssignmentResponse> listAssignmentsOfStaff(UUID storeId, UUID staffId, OrderStatus status) {
        // bảo vệ: store & staff thuộc store
        storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found"));
        var staff = staffRepo.findById(staffId).orElseThrow(() -> new NoSuchElementException("Staff not found"));
        if (!staff.getStore().getStoreId().equals(storeId)) {
            throw new SecurityException("Staff not belong to this store");
        }

        if (status == null) {
            var entities = assignmentRepo.findAllByStoreAndDeliveryStaffFetchItems(storeId, staffId);
            return entities.stream().map(this::toDTO).toList();
        } else {
            var page = assignmentRepo.findPageByStoreAndDeliveryStaffAndStatusFetchItems(
                    storeId, staffId, status, PageRequest.of(0, 1000, Sort.by("assignedAt").descending()));
            return page.getContent().stream().map(this::toDTO).toList();
        }

    }

    @Override
    @Transactional
    public Page<DeliveryAssignmentResponse> pageAssignmentsOfStaff(UUID storeId, UUID staffId, OrderStatus status, int page, int size, String sort) {
        storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found"));
        var staff = staffRepo.findById(staffId).orElseThrow(() -> new NoSuchElementException("Staff not found"));
        if (!staff.getStore().getStoreId().equals(storeId)) {
            throw new SecurityException("Staff not belong to this store");
        }

        Sort s = Sort.by((sort == null || sort.isBlank()) ? "assignedAt" : sort).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), s);

        Page<DeliveryAssignment> p =
                assignmentRepo.findPageByStoreAndDeliveryStaffAndStatusFetchItems(
                        storeId, staffId, status, pageable);
        return p.map(this::toDTO);

    }


    private DeliveryAssignmentResponse toDTO(DeliveryAssignment a) {
        if (a == null) return null;

        var so = a.getStoreOrder();
        var deliver = a.getDeliveryStaff();
        var prepared = a.getPreparedBy();

        return DeliveryAssignmentResponse.builder()
                .id(a.getId())

                .storeOrderId(so != null ? so.getId() : null)
                .orderStatus(so != null && so.getStatus() != null ? so.getStatus().name() : null)
                .shipReceiverName(so != null ? so.getShipReceiverName() : null)
                .shipPhoneNumber(so != null ? so.getShipPhoneNumber() : null)

                .deliveryStaffId(deliver != null ? deliver.getId() : null)
                .deliveryStaffName(deliver != null ? deliver.getFullName() : null)

                .preparedById(prepared != null ? prepared.getId() : null)
                .preparedByName(prepared != null ? prepared.getFullName() : null)

                .assignedAt(a.getAssignedAt())
                .pickUpAt(a.getPickUpAt())
                .deliveredAt(a.getDeliveredAt())
                .note(a.getNote())
                .items(toItemDTOs(so))
                .orderTotal(calcOrderTotal(so))
                .build();
    }

    private List<OrderItemResponse> toItemDTOs(StoreOrder so) {
        if (so == null || so.getItems() == null) return List.of();
        BigDecimal orderGrand = Optional.ofNullable(so.getGrandTotal()).orElse(BigDecimal.ZERO);
        return so.getItems().stream().map(i ->
                OrderItemResponse.builder()
                        .id(i.getId())
                        .type(i.getType())
                        .refId(i.getRefId())
                        .name(i.getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .orderGrandTotal(orderGrand)
                        .build()
        ).toList();
    }

    private BigDecimal calcOrderTotal(StoreOrder so) {
        if (so == null || so.getItems() == null) return BigDecimal.ZERO;
        return so.getItems().stream()
                .map(StoreOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
