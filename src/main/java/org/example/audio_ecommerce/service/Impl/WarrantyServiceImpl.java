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
    public WarrantyActivationResult activateForStoreOrder(UUID storeOrderId) {
        StoreOrder so = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found: " + storeOrderId));

        CustomerOrder co = so.getCustomerOrder();
        if (co == null) throw new IllegalStateException("StoreOrder has no CustomerOrder linked");

        if (!(OrderStatus.DELIVERY_SUCCESS.equals(co.getStatus()) || OrderStatus.DELIVERY_SUCCESS.equals(so.getStatus()))) {
            log.warn("[WARRANTY] activateForStoreOrder called while order not delivered yet: so={}, coStatus={}, soStatus={}",
                    storeOrderId, co.getStatus(), so.getStatus());
            // vẫn cho phép manual
        }

        if (so.getItems() == null || so.getItems().isEmpty()) {
            log.info("[WARRANTY] StoreOrder {} has no items. Skip.", storeOrderId);
            return WarrantyService.WarrantyActivationResult.builder()
                    .storeOrderId(storeOrderId)
                    .created(0).skipped(0).totalExpected(0)
                    .alreadyActivated(false).noEligibleItems(true)
                    .build();
        }

        int created = 0, skipped = 0, totalExpected = 0;
        boolean hadProduct = false;

        for (StoreOrderItem item : so.getItems()) {
            if (!"PRODUCT".equalsIgnoreCase(item.getType())) continue;
            hadProduct = true;

            UUID productId = item.getRefId();
            Product p = productRepo.findById(productId)
                    .orElseThrow(() -> new NoSuchElementException("Product not found for item: " + productId));

            List<Warranty> existing = warrantyRepo.findByStoreOrderItemId(item.getId());
            int already = existing == null ? 0 : existing.size();
            int need = Optional.ofNullable(item.getQuantity()).orElse(1);
            totalExpected += need;

            int remain = Math.max(0, need - already);
            if (remain <= 0) {
                skipped++;
                continue;
            }

            LocalDate purchase = co.getCreatedAt() != null ? co.getCreatedAt().toLocalDate() : LocalDate.now();
            Integer months = resolveMonths(p);

            for (int i = 0; i < remain; i++) {
                Warranty w = Warranty.builder()
                        .customer(co.getCustomer())
                        .store(so.getStore())
                        .product(p)
                        .storeOrderItemId(item.getId())
                        .policyCode("AUTO_FROM_ORDER")
                        .durationMonths(months)
                        .purchaseDate(purchase)
                        .startDate(purchase)
                        .status(WarrantyStatus.ACTIVE)
                        .covered(true)
                        .build();
                warrantyRepo.save(w);
                created++;
            }
        }

        boolean alreadyActivated = hadProduct && created == 0 && totalExpected > 0;

        log.info("[WARRANTY] Activation for {} => created={}, skipped={}, totalExpected={}, alreadyActivated={}",
                storeOrderId, created, skipped, totalExpected, alreadyActivated);

        return WarrantyService.WarrantyActivationResult.builder()
                .storeOrderId(storeOrderId)
                .created(created)
                .skipped(skipped)
                .totalExpected(totalExpected)
                .alreadyActivated(alreadyActivated)
                .noEligibleItems(!hadProduct)
                .build();
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

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> listByStoreOrderId(UUID storeOrderId) {
        StoreOrder so = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found: " + storeOrderId));

        if (so.getItems() == null || so.getItems().isEmpty()) return List.of();

        // chỉ lấy item PRODUCT
        List<StoreOrderItem> productItems = so.getItems().stream()
                .filter(i -> "PRODUCT".equalsIgnoreCase(i.getType()))
                .toList();
        if (productItems.isEmpty()) return List.of();

        List<UUID> itemIds = productItems.stream().map(StoreOrderItem::getId).toList();
        // group warranties theo itemId
        Map<UUID, List<Warranty>> byItemId = warrantyRepo.findByStoreOrderItemIdIn(itemIds)
                .stream()
                .collect(Collectors.groupingBy(Warranty::getStoreOrderItemId));

        List<WarrantyResponse> out = new ArrayList<>();

        for (StoreOrderItem item : productItems) {
            int qty = Optional.ofNullable(item.getQuantity()).orElse(1);
            List<Warranty> existing = byItemId.getOrDefault(item.getId(), List.of());

            // 1) add các warranty đã kích hoạt
            for (Warranty w : existing) {
                out.add(toWarrantyResponse(w));
            }

            // 2) nếu còn thiếu ⇒ đẩy “placeholder” PENDING_ACTIVATION
            int remain = Math.max(0, qty - existing.size());
            if (remain > 0) {
                // cần product + bối cảnh để build response
                Product p = productRepo.findById(item.getRefId())
                        .orElse(null); // nếu null vẫn render tối thiểu

                // ngày mua tham chiếu
                CustomerOrder co = so.getCustomerOrder();
                LocalDate purchase = (co != null && co.getCreatedAt() != null)
                        ? co.getCreatedAt().toLocalDate()
                        : LocalDate.now();

                for (int i = 0; i < remain; i++) {
                    out.add(buildPendingActivationResponse(item, so, p, purchase));
                }
            }
        }

        // (tùy chọn) sắp xếp: đã kích hoạt trước, pending sau
        out.sort(Comparator.comparing((WarrantyResponse r) -> "PENDING_ACTIVATION".equals(r.getStatus()))
                .thenComparing(r -> Optional.ofNullable(r.getProductName()).orElse("")));
        return out;
    }

    private WarrantyResponse buildPendingActivationResponse(
            StoreOrderItem item, StoreOrder so, Product p, LocalDate purchaseRef
    ) {
        return WarrantyResponse.builder()
                .id(null) // CHƯA có warranty record
                .productId(p != null ? p.getProductId() : null)
                .productName(p != null ? p.getName() : item.getName())
                .storeId(so.getStore() != null ? so.getStore().getStoreId() : null)
                .storeName(so.getStore() != null ? so.getStore().getStoreName() : null)
                .customerId(so.getCustomerOrder() != null && so.getCustomerOrder().getCustomer() != null
                        ? so.getCustomerOrder().getCustomer().getId() : null)
                .customerName(so.getCustomerOrder() != null && so.getCustomerOrder().getCustomer() != null
                        ? so.getCustomerOrder().getCustomer().getFullName() : null)
                .serialNumber(null)
                .policyCode(null)
                .durationMonths(p != null ? resolveMonths(p) : null)
                .purchaseDate(purchaseRef)
                .startDate(null) // chưa kích hoạt
                .endDate(null)
                .status("PENDING_ACTIVATION") // chỉ dùng để render UI
                .covered(false)
                .stillValid(false)
                .build();
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
