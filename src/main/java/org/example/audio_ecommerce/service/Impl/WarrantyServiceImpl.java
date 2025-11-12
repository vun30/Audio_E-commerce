package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.LogWarrantyResponse;
import org.example.audio_ecommerce.dto.response.WarrantyResponse;
import org.example.audio_ecommerce.dto.response.WarrantyReviewResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;
import org.example.audio_ecommerce.entity.Enum.WarrantyStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.WarrantyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.audio_ecommerce.service.Impl.WarrantyPolicyResolver.resolveMonths;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyServiceImpl implements WarrantyService {

    private final StoreOrderRepository storeOrderRepo;
    private final ProductRepository productRepo;
    private final CustomerOrderRepository customerOrderRepo;
    private final WarrantyRepository warrantyRepo;
    private final LogWarrantyRepository logWarrantyRepo;
    private final WarrantyPartRepository warrantyPartRepo;
    private final WarrantyReviewRepository warrantyReviewRepo;
    private final CustomerRepository customerRepo;
    private final StoreRepository storeRepo;

    @Override
    @Transactional
    public void activateForStoreOrder(UUID storeOrderId) {
        // 1) Lấy StoreOrder + kiểm tra trạng thái giao thành công (tùy policy của bạn)
        StoreOrder so = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found: " + storeOrderId));

        CustomerOrder co = so.getCustomerOrder();
        if (co == null) throw new IllegalStateException("StoreOrder has no CustomerOrder linked");
        if (!(OrderStatus.DELIVERY_SUCCESS.equals(co.getStatus()) || OrderStatus.DELIVERY_SUCCESS.equals(so.getStatus()))) {
            log.warn("[WARRANTY] activateForStoreOrder called while order not delivered yet: so={}, coStatus={}, soStatus={}",
                    storeOrderId, co.getStatus(), so.getStatus());
            // tùy bạn: có thể throw, hoặc cho phép manual-activate. Mình CHO PHÉP manual ⇒ không throw.
        }

        // 2) Duyệt từng item PRODUCT, tạo N warranty theo quantity (idempotent)
        if (so.getItems() == null || so.getItems().isEmpty()) {
            log.info("[WARRANTY] StoreOrder {} has no items. Skip.", storeOrderId);
            return;
        }

        int created = 0, skipped = 0;
        for (StoreOrderItem item : so.getItems()) {
            if (!"PRODUCT".equalsIgnoreCase(item.getType())) {
                continue; // bỏ qua COMBO
            }

            UUID productId = item.getRefId();
            Product p = productRepo.findById(productId)
                    .orElseThrow(() -> new NoSuchElementException("Product not found for item: " + productId));

            // Idempotent theo storeOrderItemId: đã tạo đủ số lượng chưa?
            List<Warranty> existing = warrantyRepo.findByStoreOrderItemId(item.getId());
            int already = existing == null ? 0 : existing.size();
            int need = Optional.ofNullable(item.getQuantity()).orElse(1);
            int remain = Math.max(0, need - already);
            if (remain <= 0) {
                skipped++;
                continue;
            }

            // Ngày mua: theo CustomerOrder.createdAt (có thể đổi sang ngày giao thành công nếu bạn có cột)
            LocalDate purchase = co.getCreatedAt() != null ? co.getCreatedAt().toLocalDate() : LocalDate.now();

            Integer months = resolveMonths(p); // helper parse "24 tháng" → 24, default 12
            for (int i = 0; i < remain; i++) {
                Warranty w = Warranty.builder()
                        .customer(co.getCustomer())
                        .store(so.getStore())
                        .product(p)
                        .storeOrderItemId(item.getId())
                        .policyCode("AUTO_FROM_ORDER")   // gắn nhãn nguồn
                        .durationMonths(months)
                        .purchaseDate(purchase)
                        .startDate(purchase)
                        .status(WarrantyStatus.ACTIVE)    // nếu bạn có enum trạng thái
                        .covered(true)                    // ban đầu coi như còn hạn (sẽ tính lại khi trả response)
                        .build();
                warrantyRepo.save(w);
                created++;
            }
        }

        log.info("[WARRANTY] Activation done for StoreOrder {}: created={}, skippedExisting={}", storeOrderId, created, skipped);
    }

    @Override
    @Transactional
    public Optional<WarrantyResponse> activateSingleItem(StoreOrderItem item) {
        if (item == null || !"PRODUCT".equalsIgnoreCase(item.getType())) return Optional.empty();
        Product p = productRepo.findById(item.getRefId()).orElse(null);
        if (p == null) return Optional.empty();

        StoreOrder so = item.getStoreOrder();
        CustomerOrder co = so.getCustomerOrder();
        Customer customer = co.getCustomer();

        LocalDate purchase = co.getCreatedAt() != null ? co.getCreatedAt().toLocalDate() : LocalDate.now();

        Warranty w = Warranty.builder()
                .customer(customer)
                .store(so.getStore())
                .product(p)
                .storeOrderItemId(item.getId())
                .policyCode("MANUAL")
                .durationMonths(resolveMonths(p))
                .purchaseDate(purchase)
                .startDate(purchase)
                .build();
        Warranty saved = warrantyRepo.save(w);
        return Optional.of(toWarrantyResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> search(WarrantySearchRequest req) {
        if (req == null) return List.of();

        if (req.getSerial() != null && !req.getSerial().isBlank()) {
            return warrantyRepo.findBySerialNumber(req.getSerial())
                    .map(w -> List.of(toWarrantyResponse(w)))
                    .orElse(List.of());
        }

        if (req.getOrderId() != null) {
            CustomerOrder co = customerOrderRepo.findById(req.getOrderId())
                    .orElseThrow(() -> new NoSuchElementException("CustomerOrder not found"));
            Set<UUID> itemIds = co.getItems().stream()
                    .map(CustomerOrderItem::getId).collect(Collectors.toSet());

            return warrantyRepo.findAll().stream()
                    .filter(w -> w.getStoreOrderItemId() != null && itemIds.contains(w.getStoreOrderItemId()))
                    .map(this::toWarrantyResponse)
                    .toList();
        }

        if (req.getPhoneOrEmail() != null) {
            String s = req.getPhoneOrEmail().trim().toLowerCase();
            List<Customer> candidates = customerRepo.findAll().stream()
                    .filter(c -> (c.getPhoneNumber()!=null && c.getPhoneNumber().equalsIgnoreCase(s))
                            || (c.getEmail()!=null && c.getEmail().toLowerCase().equals(s)))
                    .toList();
            return candidates.stream()
                    .flatMap(c -> warrantyRepo.findByCustomer(c).stream())
                    .map(this::toWarrantyResponse)
                    .toList();
        }

        return List.of();
    }

    @Override
    @Transactional
    public LogWarrantyResponse openTicket(UUID warrantyId, WarrantyLogOpenRequest req) {
        Warranty w = warrantyRepo.findById(warrantyId)
                .orElseThrow(() -> new NoSuchElementException("Warranty not found"));

        Boolean covered = req.getCovered();
        if (covered == null) {
            covered = evaluateCoverage(w);
        }

        LogWarranty logW = LogWarranty.builder()
                .warranty(w)
                .status(WarrantyLogStatus.OPEN)
                .problemDescription(req.getProblemDescription())
                .covered(covered)
                .attachmentsJson(req.getAttachmentUrls()==null? null : String.join("\n", req.getAttachmentUrls()))
                .build();

        return toLogResponse(logWarrantyRepo.save(logW));
    }

    @Override
    @Transactional
    public LogWarrantyResponse updateTicketStatus(UUID logId, WarrantyLogStatus newStatus, WarrantyLogUpdateRequest req) {
        LogWarranty logW = logWarrantyRepo.findById(logId)
                .orElseThrow(() -> new NoSuchElementException("Warranty log not found"));

        if (req != null) {
            if (req.getDiagnosis()!=null) logW.setDiagnosis(req.getDiagnosis());
            if (req.getResolution()!=null) logW.setResolution(req.getResolution());
            if (req.getShipBackTracking()!=null) logW.setShipBackTracking(req.getShipBackTracking());
            if (req.getAttachmentUrls()!=null) logW.setAttachmentsJson(String.join("\n", req.getAttachmentUrls()));
            if (req.getCostLabor()!=null) logW.setCostLabor(req.getCostLabor());
            if (req.getCostParts()!=null) logW.setCostParts(req.getCostParts());
        }
        logW.setStatus(newStatus);
        return toLogResponse(logWarrantyRepo.save(logW));
    }

    @Override
    @Transactional
    public WarrantyResponse setSerialFirstTime(UUID warrantyId, String serial, String note) {
        Warranty w = warrantyRepo.findById(warrantyId)
                .orElseThrow(() -> new NoSuchElementException("Warranty not found"));
        if (w.getSerialNumber() != null && !w.getSerialNumber().isBlank()) {
            throw new IllegalStateException("Serial already set");
        }
        w.setSerialNumber(serial);
        if (note != null) w.setNotes((w.getNotes()==null?"":w.getNotes()+"\n")+"Activate: "+note);
        return toWarrantyResponse(warrantyRepo.save(w));
    }

    @Override
    @Transactional
    public WarrantyReviewResponse review(UUID logId, UUID customerId, WarrantyReviewRequest req) {
        LogWarranty logW = logWarrantyRepo.findById(logId)
                .orElseThrow(() -> new NoSuchElementException("Warranty log not found"));
        Warranty w = logW.getWarranty();
        if (!w.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Customer mismatch");
        }
        WarrantyReview rv = WarrantyReview.builder()
                .warranty(w)
                .log(logW)
                .customer(w.getCustomer())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        return toReviewResponse(warrantyReviewRepo.save(rv));
    }

    private WarrantyResponse toWarrantyResponse(Warranty w) {
        boolean stillValid = evaluateCoverage(w);
        return WarrantyResponse.builder()
                .id(w.getId())
                .productId(w.getProduct() != null ? w.getProduct().getProductId() : null)
                .productName(w.getProduct() != null ? w.getProduct().getName() : null)
                .storeId(w.getStore() != null ? w.getStore().getStoreId() : null)
                .storeName(w.getStore() != null ? w.getStore().getStoreName() : null)
                .customerId(w.getCustomer() != null ? w.getCustomer().getId() : null)
                .customerName(w.getCustomer() != null ? w.getCustomer().getFullName() : null)
                .serialNumber(w.getSerialNumber())
                .policyCode(w.getPolicyCode())
                .durationMonths(w.getDurationMonths())
                .purchaseDate(w.getPurchaseDate())
                .startDate(w.getStartDate())
                .endDate(w.getStartDate() != null ? w.getStartDate().plusMonths(w.getDurationMonths()) : null)
                .status(w.getStatus() != null ? w.getStatus().name() : null)
                .covered(w.isCovered())
                .stillValid(stillValid)
                .build();
    }

    private LogWarrantyResponse toLogResponse(LogWarranty log) {
        return LogWarrantyResponse.builder()
                .id(log.getId())
                .warrantyId(log.getWarranty() != null ? log.getWarranty().getId() : null)
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .problemDescription(log.getProblemDescription())
                .diagnosis(log.getDiagnosis())
                .resolution(log.getResolution())
                .covered(log.getCovered())
                .costLabor(log.getCostLabor())
                .costParts(log.getCostParts())
                .costTotal(log.getCostTotal())
                .attachmentUrls(log.getAttachmentsJson() != null ?
                        Arrays.asList(log.getAttachmentsJson().split("\n")) : List.of())
                .shipBackTracking(log.getShipBackTracking())
                .createdAt(log.getOpenedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }

    private WarrantyReviewResponse toReviewResponse(WarrantyReview r) {
        return WarrantyReviewResponse.builder()
                .id(r.getId())
                .warrantyId(r.getWarranty() != null ? r.getWarranty().getId() : null)
                .logId(r.getLog() != null ? r.getLog().getId() : null)
                .customerId(r.getCustomer() != null ? r.getCustomer().getId() : null)
                .customerName(r.getCustomer() != null ? r.getCustomer().getFullName() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private boolean evaluateCoverage(Warranty w) {
        if (w.getStartDate() == null || w.getDurationMonths() == null) return true; // fallback: coi như còn hạn
        LocalDate end = w.getStartDate().plusMonths(w.getDurationMonths());
        LocalDate now = LocalDate.now();
        return !now.isAfter(end); // true nếu hôm nay <= ngày hết hạn
    }
}
