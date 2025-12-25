package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.ReturnPackageFeeResponse;
import org.example.audio_ecommerce.dto.response.ReturnPreviewResponse;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.ReturnFaultType;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.*;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository returnRepo;
    private final ReturnShippingFeeRepository shippingFeeRepo;
    private final SecurityUtils securityUtils;
    private final WalletService walletService;
    private final StoreRepository storeRepo;
    private final CustomerAddressRepository customerAddressRepo;
    private final CustomerOrderItemRepository customerOrderItemRepo;
    private final GhnFeeService ghnFeeService;
    private final GhnOrderService ghnOrderService;
    private final RestTemplate restTemplate;
    private final StoreOrderRepository storeOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalPointService legalPointService;
    private final CustomerRepository customerRepo;

    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shopId}")
    private String ghnShopId;

    private static final String GHN_BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";

    // ================== Helper ==================

    private ReturnRequestResponse toResponse(ReturnRequest r) {
        // ===== STORE =====
        Store store = null;
        if (r.getShopId() != null) {
            store = storeRepo.findById(r.getShopId()).orElse(null);
        }

        // ===== CUSTOMER =====
        Customer customer = null;
        if (r.getCustomerId() != null) {
            customer = customerRepo.findById(r.getCustomerId()).orElse(null);
        }

        String orderCode = null;

        if (r.getOrderItemId() != null) {
            CustomerOrderItem item = customerOrderItemRepo.findById(r.getOrderItemId()).orElse(null);
            if (item != null && item.getCustomerOrder() != null) {
                orderCode = item.getCustomerOrder().getOrderCode(); // ✅ mã đơn hệ thống
            }
        }
        return ReturnRequestResponse.builder()
                .id(r.getId())
                .customerId(r.getCustomerId())
                .shopId(r.getShopId())
                .escalatedById(r.getStatus() == ReturnStatus.DISPUTE ? r.getShopId() : null)
                .escalatedByName(r.getStatus() == ReturnStatus.DISPUTE && store != null ? store.getStoreName() : null)
                .escalatedByRole(r.getStatus() == ReturnStatus.DISPUTE ? "SHOP" : "CUSTOMER")
                .orderItemId(r.getOrderItemId())
                .productId(r.getProductId())
                .productName(r.getProductName())
                .itemPrice(r.getItemPrice())
                .storeName(store != null ? store.getStoreName() : null)
                .storeLegalPoint(store != null ? store.getLegalPoint() : BigDecimal.ZERO)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerLegalPoint(customer != null ? customer.getLegalPoint() : BigDecimal.ZERO)
                .orderCode(orderCode)
                .reasonType(r.getReasonType())
                .reason(r.getReason())
                .customerImageUrls(r.getCustomerImageUrls())
                .customerVideoUrl(r.getCustomerVideoUrl())
                .status(r.getStatus())
                .faultType(r.getFaultType())
                .packageWeight(r.getPackageWeight())
                .packageLength(r.getPackageLength())
                .packageWidth(r.getPackageWidth())
                .packageHeight(r.getPackageHeight())
                .shippingFee(r.getShippingFee())
                .ghnOrderCode(r.getGhnOrderCode())
                .trackingStatus(r.getTrackingStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .adminForcedContinue(r.isAdminForcedContinue())
                .finalDecision(r.isFinalDecision())
                .shopDisputeReason(r.getShopDisputeReason())
                .build();
    }

    private HttpHeaders ghnHeaders(boolean includeShopId) {
        HttpHeaders h = new HttpHeaders();
        h.set("Token", ghnToken);
        if (includeShopId) h.set("ShopId", ghnShopId);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // =========================================================
    // ====================== CUSTOMER =========================
    // =========================================================

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(ReturnRequestCreateRequest req) {
        UUID customerId = securityUtils.getCurrentCustomerId();

        // 1️⃣ Lấy snapshot từ CustomerOrderItem
        CustomerOrderItem orderItem = customerOrderItemRepo.findById(req.getOrderItemId())
                .orElseThrow(() -> new NoSuchElementException("Order item not found"));

        if (!orderItem.getCustomerOrder().getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("Order item does not belong to current customer");
        }

        UUID shopId = orderItem.getStoreId();
        UUID productId = orderItem.getRefId();
        String productName = orderItem.getName();
        BigDecimal itemPrice = orderItem.getFinalLineTotal(); // hoặc lineTotal nếu hoàn theo cả dòng

        // 2️⃣ Tạo ReturnRequest
        ReturnRequest entity = ReturnRequest.builder()
                .customerId(customerId)
                .shopId(shopId)
                .orderItemId(orderItem.getId())
                .productId(productId)
                .productName(productName)
                .itemPrice(itemPrice)
                .reasonType(req.getReasonType())
                .reason(req.getReason())
                .customerVideoUrl(req.getCustomerVideoUrl())
                .customerImageUrls(
                        Optional.ofNullable(req.getCustomerImageUrls())
                                .orElseGet(ArrayList::new)
                )
                .status(ReturnStatus.PENDING)
                .faultType(ReturnFaultType.UNKNOWN)
                .build();

        entity = returnRepo.save(entity);

        // 3️⃣ Cập nhật status CustomerOrder + StoreOrder
        CustomerOrder customerOrder = orderItem.getCustomerOrder();

        // chỉ đổi trạng thái nếu đơn đã giao thành công
        if (customerOrder.getStatus() == OrderStatus.DELIVERY_SUCCESS || customerOrder.getStatus() == OrderStatus.COMPLETED) {
            customerOrder.setStatus(OrderStatus.RETURN_REQUESTED);
            customerOrder.setCreatedAt(LocalDateTime.now()); // nếu có field này
            customerOrderRepository.save(customerOrder);
        }

        // Tìm storeOrder tương ứng với shopId của item này
        StoreOrder targetStoreOrder = storeOrderRepository
                .findAllByCustomerOrder_Id(customerOrder.getId()).stream()
                .filter(so -> so.getStore() != null
                        && so.getStore().getStoreId().equals(shopId))
                .findFirst()
                .orElse(null);

        if (targetStoreOrder != null &&
                targetStoreOrder.getStatus() == OrderStatus.DELIVERY_SUCCESS) {

            targetStoreOrder.setStatus(OrderStatus.RETURN_REQUESTED);
            targetStoreOrder.setCreatedAt(LocalDateTime.now()); // nếu em có field
            storeOrderRepository.save(targetStoreOrder);
        }

        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listForCurrentCustomer(Pageable pageable) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        return returnRepo.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ReturnPackageFeeResponse setPackageInfoAndCalculateFee(
            UUID returnRequestId,
            ReturnPackageInfoRequest req
    ) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));
        if (Boolean.TRUE.equals(r.isFinalDecision())) {
            throw new IllegalStateException("Return request is closed by admin decision.");
        }

        if (!r.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Not your return request");
        }

        if (r.getStatus() != ReturnStatus.APPROVED) {
            throw new IllegalStateException("ReturnRequest must be APPROVED");
        }

        // Validate package dims
        if (req.getWeight() == null || req.getWeight().compareTo(BigDecimal.ZERO) <= 0
                || req.getLength() == null || req.getLength().compareTo(BigDecimal.ZERO) <= 0
                || req.getWidth() == null || req.getWidth().compareTo(BigDecimal.ZERO) <= 0
                || req.getHeight() == null || req.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid package dimensions/weight");
        }

        // 1️⃣ Lấy địa chỉ pickup CUSTOMER
        CustomerAddress pickupAddr;
        if (req.getCustomerAddressId() != null) {
            pickupAddr = customerAddressRepo.findById(req.getCustomerAddressId())
                    .orElseThrow(() -> new NoSuchElementException("Customer address not found"));
            if (!pickupAddr.getCustomer().getId().equals(customerId)) {
                throw new AccessDeniedException("Address does not belong to current customer");
            }
        } else {
            // default address / ưu tiên isDefault
            List<CustomerAddress> addresses =
                    customerAddressRepo.findByCustomer_IdOrderByIsDefaultDescCreatedAtDesc(customerId);
            if (addresses.isEmpty()) {
                throw new IllegalStateException("Customer has no shipping address");
            }
            pickupAddr = addresses.get(0);
        }

        if (pickupAddr.getDistrictId() == null || pickupAddr.getWardCode() == null) {
            throw new IllegalStateException("Customer address missing GHN districtId/wardCode");
        }

        // 2️⃣ Lấy địa chỉ kho của store (TO)
        if (r.getShopId() == null) {
            throw new IllegalStateException("ReturnRequest missing shopId");
        }

        Store store = storeRepo.findById(r.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Store not found"));

        StoreAddressEntity storeAddr;
        if (req.getStoreAddressId() != null) {
            storeAddr = store.getStoreAddresses().stream()
                    .filter(a -> a.getId().equals(req.getStoreAddressId()))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Store address not found"));
        } else {
            // lấy địa chỉ default hoặc bất kỳ
            storeAddr = store.getStoreAddresses().stream()
                    .sorted((a, b) -> Boolean.compare(
                            Boolean.FALSE.equals(b.getDefaultAddress()),
                            Boolean.FALSE.equals(a.getDefaultAddress())
                    ))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Store has no warehouse address"));
        }

        if (storeAddr.getDistrictCode() == null || storeAddr.getWardCode() == null) {
            throw new IllegalStateException("Store address missing GHN district/ward code");
        }

        Integer toDistrictId;
        try {
            toDistrictId = Integer.parseInt(storeAddr.getDistrictCode());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Store districtCode is not valid GHN district_id", ex);
        }

        // 3️⃣ Build GhnFeeRequest
        GhnFeeRequest feeReq = new GhnFeeRequest();
        feeReq.setService_type_id(2); // hàng nặng (tuỳ cấu hình)
        feeReq.setFrom_district_id(pickupAddr.getDistrictId());
        feeReq.setFrom_ward_code(pickupAddr.getWardCode());
        feeReq.setTo_district_id(toDistrictId);
        feeReq.setTo_ward_code(storeAddr.getWardCode());
        
        // Ensure dimensions are valid positive integers
        int length = Math.max(req.getLength().intValue(), 1);
        int width = Math.max(req.getWidth().intValue(), 1);
        int height = Math.max(req.getHeight().intValue(), 1);
        // Convert weight to grams and ensure it's a positive value
        // Also ensure it doesn't exceed reasonable limits (GHN might have upper limits)
        int weightInGrams = req.getWeight().multiply(BigDecimal.valueOf(1000)).intValue();
        int weight = Math.max(Math.min(weightInGrams, 50000), 1); // Clamp between 1g and 50kg (50000g)
        
        feeReq.setLength(length);
        feeReq.setWidth(width);
        feeReq.setHeight(height);
        feeReq.setWeight(weight);
        feeReq.setInsurance_value(0);
        feeReq.setCoupon(null);
        feeReq.setItems(null); // không bắt buộc

        // 4️⃣ Call GHN Fee API
        BigDecimal fee;
        try {
            String rawJson = ghnFeeService.calculateFeeRaw(feeReq);
            JsonNode root = objectMapper.readTree(rawJson);

            if (root.path("code").asInt() != 200) {
                log.warn("[RETURN FEE] GHN fee error: {}", rawJson);
                throw new IllegalStateException("GHN fee API failed: " + root.path("message").asText());
            }

            JsonNode data = root.path("data");
            if (data == null || data.isMissingNode()) {
                throw new IllegalStateException("GHN fee API missing data");
            }

            if (data.hasNonNull("total")) {
                fee = new BigDecimal(data.get("total").asText());
            } else if (data.hasNonNull("service_fee")) {
                fee = new BigDecimal(data.get("service_fee").asText());
            } else {
                throw new IllegalStateException("GHN fee API missing total/service_fee");
            }
        } catch (Exception e) {
            log.error("[RETURN FEE] Failed calculate GHN return fee for request {}: {}", 
                    returnRequestId, e.getMessage(), e);
            // Add more detailed logging to help debug the issue
            log.error("[RETURN FEE] Request details - Length: {}, Width: {}, Height: {}, Weight: {}", 
                    length, width, height, weight);
            log.error("[RETURN FEE] District IDs - From: {}, To: {}", 
                    pickupAddr.getDistrictId(), toDistrictId);
            log.error("[RETURN FEE] Ward Codes - From: {}, To: {}", 
                    pickupAddr.getWardCode(), storeAddr.getWardCode());
            throw new RuntimeException("Failed to calculate return shipping fee", e);
        }

        // 5️⃣ Lưu package + fee + snapshot địa chỉ pickup vào ReturnRequest
        r.setPackageWeight(req.getWeight());
        r.setPackageLength(req.getLength());
        r.setPackageWidth(req.getWidth());
        r.setPackageHeight(req.getHeight());
        r.setShippingFee(fee);

        r.setPickupWardCode(pickupAddr.getWardCode());
        r.setPickupWardName(pickupAddr.getWard());
        r.setPickupDistrictCode(
                pickupAddr.getDistrictId() != null ? pickupAddr.getDistrictId().toString() : null
        );
        r.setPickupDistrictName(pickupAddr.getDistrict());
        r.setPickupProvinceCode(pickupAddr.getProvinceCode());
        r.setPickupProvinceName(pickupAddr.getProvince());
        r.setPickupAddressLine(
                pickupAddr.getAddressLine() != null
                        ? pickupAddr.getAddressLine()
                        : pickupAddr.getStreet()
        );
        r.setCustomerPhone(pickupAddr.getPhoneNumber());

        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);
        fillFeeWhenCustomerHasPackage(r, fee);

        return ReturnPackageFeeResponse.builder()
                .shippingFee(fee)
                .build();
    }

    // =========================================================
    // ======================== SHOP ===========================
    // =========================================================
    @Override
    @Transactional
    public ReturnRequestResponse disputeToAdmin(UUID returnRequestId, ReturnDisputeRequest req) {

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new RuntimeException("ReturnRequest not found: " + returnRequestId));

        // ❗ guard này của bạn đang sai message (finalDecision=true là shop thắng/case đóng),
        // nhưng thôi, giữ nguyên logic chặn nếu bạn muốn.
        if (Boolean.TRUE.equals(r.isFinalDecision())) {
            throw new IllegalStateException("Return request is closed by admin decision.");
        }

        UUID currentShopId = securityUtils.getCurrentStoreId();
        if (!r.getShopId().equals(currentShopId)) {
            throw new AccessDeniedException("Not your return request");
        }

        // ✅ Set dispute info
        r.setStatus(ReturnStatus.DISPUTE);
        r.setShopDisputeReason(req != null ? req.getReason() : null);

        // ✅ KHÔNG lấy ảnh/video từ shop nữa → copy từ customer
        r.setShopVideoUrl(r.getCustomerVideoUrl());
        r.setShopImageUrls(
                r.getCustomerImageUrls() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(r.getCustomerImageUrls())
        );


        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);

        return toResponse(r);
    }




    @Override
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listForCurrentShop(Pageable pageable) {
        UUID shopId = securityUtils.getCurrentStoreId();
        return returnRepo.findByShopId(shopId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void approveReturnByShop(UUID returnRequestId) {
        UUID shopId = securityUtils.getCurrentStoreId();
        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!r.getShopId().equals(shopId)) {
            throw new AccessDeniedException("Not your return request");
        }
        if (r.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("ReturnRequest must be PENDING");
        }

        r.setStatus(ReturnStatus.APPROVED);
        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);
    }

    @Override
    @Transactional
    public ReturnRequestResponse createGhnReturnOrderByShop(UUID returnRequestId, ReturnCreateGhnOrderRequest req) {
        UUID shopId = securityUtils.getCurrentStoreId();
        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!r.getShopId().equals(shopId)) {
            throw new AccessDeniedException("Not your return request");
        }
        if (r.getStatus() != ReturnStatus.APPROVED) {
            throw new IllegalStateException("ReturnRequest must be APPROVED");
        }
        if (r.getShippingFee() == null) {
            throw new IllegalStateException("ReturnRequest must have shipping fee calculated first");
        }

        // 🔹 Tìm CustomerOrderItem từ orderItemId đã lưu trong ReturnRequest
        CustomerOrderItem orderItem = customerOrderItemRepo.findById(r.getOrderItemId())
                .orElseThrow(() -> new NoSuchElementException("Order item not found for return request"));

        // 🔹 Lấy CustomerOrder từ CustomerOrderItem
        CustomerOrder customerOrder = orderItem.getCustomerOrder();
        if (customerOrder == null) {
            throw new IllegalStateException("CustomerOrderItem has no CustomerOrder linked");
        }

        // 🔹 Tìm đúng StoreOrder của đơn này cho shop hiện tại
        List<StoreOrder> storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(customerOrder.getId());
        StoreOrder targetStoreOrder = storeOrders.stream()
                .filter(so -> so.getStore() != null
                        && so.getStore().getStoreId().equals(r.getShopId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "StoreOrder not found for this return (orderId=" + customerOrder.getId()
                                + ", shopId=" + r.getShopId() + ")"));

        UUID storeOrderId = targetStoreOrder.getId();

        // cần snapshot pickup đã có
        if (r.getPickupWardCode() == null || r.getPickupDistrictCode() == null ||
                r.getPickupProvinceName() == null || r.getPickupAddressLine() == null) {
            throw new IllegalStateException("ReturnRequest missing pickup address snapshot");
        }

        // 1️⃣ Lấy store + store address default (kho nhận hàng)
        Store store = storeRepo.findById(r.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Store not found"));

        StoreAddressEntity storeAddr = store.getStoreAddresses().stream()
                .sorted((a, b) -> Boolean.compare(
                        Boolean.FALSE.equals(b.getDefaultAddress()),
                        Boolean.FALSE.equals(a.getDefaultAddress())
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Store has no warehouse address"));

        Integer toDistrictId;
        try {
            toDistrictId = Integer.parseInt(storeAddr.getDistrictCode());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Store districtCode is not valid GHN district_id", ex);
        }

        // 2️⃣ Build body GHN create-order (return: customer → shop)
        GhnCreateOrderRequest body = GhnCreateOrderRequest.builder().build();

        // FROM = CUSTOMER
        body.setFrom_name(r.getProductName() != null ? "Customer return - " + r.getProductName() : "Customer");
        body.setFrom_phone(r.getCustomerPhone());
        body.setFrom_address(r.getPickupAddressLine());
        body.setFrom_ward_name(r.getPickupWardName());
        body.setFrom_district_name(r.getPickupDistrictName());
        body.setFrom_province_name(r.getPickupProvinceName());

        // TO = SHOP
        body.setTo_name(store.getStoreName());
        body.setTo_phone(store.getPhoneNumber());
        body.setTo_address(storeAddr.getAddress());
        body.setTo_ward_code(storeAddr.getWardCode());
        body.setTo_district_id(toDistrictId);

        // RETURN (optional, có thể set về kho luôn)
        body.setReturn_phone(store.getPhoneNumber());
        body.setReturn_address(storeAddr.getAddress());
        body.setReturn_district_id(toDistrictId);
        body.setReturn_ward_code(storeAddr.getWardCode());

        // COD = 0 (không thu hộ)
        body.setCod_amount(0);
        body.setContent("Return hàng đơn: " + r.getId());

        String payer = "SHOP";
        body.setPayment_type_id(1);


        // Service & kích thước
        body.setService_type_id(2);
        body.setWeight(r.getPackageWeight().intValue());
        body.setLength(r.getPackageLength().intValue());
        body.setWidth(r.getPackageWidth().intValue());
        body.setHeight(r.getPackageHeight().intValue());

        body.setRequired_note("KHONGCHOXEMHANG");
        body.setNote("Return hàng đơn: " + r.getId());

        if (req != null && req.getPickShiftId() != null) {
            body.setPick_shift(List.of(req.getPickShiftId()));
        }

        // Items
        GhnItem item = GhnItem.builder()
                .name(r.getProductName())
                .code(r.getProductId() != null ? r.getProductId().toString() : null)
                .quantity(1)
                .weight(r.getPackageWeight().intValue())
                .length(r.getPackageLength().intValue())
                .width(r.getPackageWidth().intValue())
                .height(r.getPackageHeight().intValue())
                .build();
        body.setItems(List.of(item));

        // 3️⃣ Call GHN create-order
        String orderCode;
        BigDecimal totalFee = BigDecimal.ZERO;
        LocalDateTime expectedDeliveryTime = null;

        try {
            org.springframework.http.HttpEntity<GhnCreateOrderRequest> entity =
                    new org.springframework.http.HttpEntity<>(body, ghnHeaders(true));

            org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                    GHN_BASE_URL + "/v2/shipping-order/create",
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("[GHN RETURN] create order failed, httpStatus={}, body={}",
                        resp.getStatusCode(), resp.getBody());
                throw new IllegalStateException("GHN create-order failed, httpStatus=" + resp.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.path("code").asInt() != 200) {
                log.error("[GHN RETURN] create order error: {}", resp.getBody());
                throw new IllegalStateException("GHN create-order error: " + root.path("message").asText());
            }

            JsonNode data = root.path("data");
            orderCode = data.path("order_code").asText();

            if (data.hasNonNull("total_fee")) {
                totalFee = new BigDecimal(data.get("total_fee").asText());
            }
            if (data.hasNonNull("expected_delivery_time")) {
                String raw = data.get("expected_delivery_time").asText();
                // GHN trả ISO (có Z), em parse theo pattern của project (có thể chỉnh lại)
                expectedDeliveryTime = LocalDateTime.parse(raw.replace("Z", ""));
            }

        } catch (Exception e) {
            log.error("[GHN RETURN] Failed to create GHN return order for request {}: {}",
                    returnRequestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create GHN return order", e);
        }

        // 4️⃣ Cập nhật ReturnRequest
        r.setGhnOrderCode(orderCode);
        r.setStatus(ReturnStatus.SHIPPING);
        r.setTrackingStatus("CREATED_WAITING_SYNC");
        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);

        // 5️⃣ Lưu GHN_ORDER (nếu muốn track chung)
        try {
            CreateGhnOrderRequest ghiReq = CreateGhnOrderRequest.builder()
                    .storeOrderId(storeOrderId)              // return nên không gắn storeOrderId cụ thể
                    .storeId(r.getShopId())
                    .orderGhn(orderCode)
                    .totalFee(totalFee)
                    .expectedDeliveryTime(expectedDeliveryTime)
                    .status("ready_to_pick")        // cho vui, impl hiện tại ignore và set READY_PICKUP
                    .build();
            ghnOrderService.create(ghiReq);
        } catch (Exception ex) {
            log.error("[GHN RETURN] Failed to persist GhnOrder for return request {}: {}",
                    returnRequestId, ex.getMessage(), ex);
        }

        // 6️⃣ Update ReturnShippingFee (không tạo mới)
        BigDecimal feeValue = (r.getShippingFee() != null) ? r.getShippingFee() : totalFee;

        ReturnShippingFee feeLog = ensureReturnShippingFeeRow(r); // ✅ lấy placeholder nếu có, không thì tạo
        feeLog.setGhnOrderCode(orderCode);
        feeLog.setShippingFee(feeValue);

        // payer theo reasonType (tạm) — nếu admin đã phán trước đó thì bạn có thể chọn GIỮ payer admin.
        // Nếu muốn giữ payer admin: chỉ set payer nếu payer hiện tại null.
        String payerFromReason = payerFromReasonType(r.getReasonType());
        if (feeLog.getPayer() == null) {
            feeLog.setPayer(payerFromReason);
        }
        String payerFinal = feeLog.getPayer();

        if ("SHOP".equalsIgnoreCase(payerFinal)) {
            feeLog.setChargedToShop(feeValue);
            feeLog.setPaidByShop(true);
            feeLog.setShopFault(true);
        } else {
            feeLog.setChargedToShop(BigDecimal.ZERO);
            feeLog.setPaidByShop(false);
            feeLog.setShopFault(false);
        }


        shippingFeeRepo.save(feeLog);



        return toResponse(r);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shopReceiveOrDispute(
            UUID returnRequestId,
            ReturnShopReceiveRequest req
    ) {
        UUID shopId = securityUtils.getCurrentStoreId();

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));
        if (Boolean.TRUE.equals(r.isFinalDecision())) {
            throw new IllegalStateException("Admin decided CUSTOMER wins. Shop must continue return flow; dispute is not allowed.");
        }


        if (!r.getShopId().equals(shopId)) {
            throw new AccessDeniedException("Not your return request");
        }

        if (r.getStatus() != ReturnStatus.SHIPPING) {
            throw new IllegalStateException("ReturnRequest must be SHIPPING");
        }

        // ✅ Trường hợp shop nhận đúng hàng, đồng ý hoàn
        if (Boolean.TRUE.equals(req.getReceivedCorrect())) {
            r.setStatus(ReturnStatus.REFUNDED);
            r.setFaultType(
                    r.getReasonType() == ReturnReasonType.CUSTOMER_FAULT
                            ? ReturnFaultType.CUSTOMER
                            : ReturnFaultType.SHOP
            );
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            // Hoàn tiền ví cho customer
            refundAndDeductLegalPointIfNeeded(r);
        } else {
            r.setStatus(ReturnStatus.DISPUTE);

            // ✅ Copy evidence từ customer
            r.setShopVideoUrl(r.getCustomerVideoUrl());
            r.setShopImageUrls(
                    r.getCustomerImageUrls() == null
                            ? new ArrayList<>()
                            : new ArrayList<>(r.getCustomerImageUrls())
            );


            // ✅ shop vẫn được nhập lý do
            r.setShopDisputeReason(req.getShopDisputeReason());

            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);
        }


        return toResponse(r);
    }

    // =========================================================
    // ======================== ADMIN ==========================
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listDispute(Pageable pageable) {
        return returnRepo.findByStatus(ReturnStatus.DISPUTE, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @Override
    public ReturnRequestResponse resolveDispute(UUID returnRequestId, ReturnDisputeResolveRequest req) {

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (r.getStatus() != ReturnStatus.DISPUTE) {
            throw new IllegalStateException("Only DISPUTE can be resolved by admin");
        }

        // lưu phán quyết
        r.setFaultType(req.getFaultType()); // SHOP / CUSTOMER
        r.setUpdatedAt(LocalDateTime.now());

        // =================================================
        // ✅ CASE 1: SHOP THẮNG → ĐÓNG LUỒNG, KHÔNG RETURN
        // =================================================
        if (req.getFaultType() == ReturnFaultType.CUSTOMER) {
            r.setStatus(ReturnStatus.DISPUTE_RESOLVED_SHOP);
            r.setFinalDecision(true);
            r.setFinalDecisionAt(LocalDateTime.now());
            r.setAdminForcedContinue(false);

            return toResponse(returnRepo.save(r));
        }

        // =================================================
        // ✅ CASE 2: CUSTOMER THẮNG → QUAY VỀ FLOW BÌNH THƯỜNG
        //     => status phải là APPROVED để customer set package và shop tạo GHN
        // =================================================
        r.setStatus(ReturnStatus.APPROVED);          // 🔥 QUAN TRỌNG
        r.setFinalDecision(false);
        r.setAdminForcedContinue(true);

        // admin phán shop chịu phí ship return (ghi vào ReturnShippingFee)
        applyAdminPayerToReturnShippingFee(r);

        return toResponse(returnRepo.save(r));
    }




    // =========================================================
    // ========================= AUTO ==========================
    // =========================================================

    /**
     * CASE 3 — SHOP KHÔNG KHIẾU NẠI DÙ KHÁCH SAI
     *
     * Ngữ cảnh:
     *  - GHN đã giao hàng trả về cho shop
     *  - status của ReturnRequest vẫn là SHIPPING
     *  - Shop KHÔNG bấm "Khiếu nại" (DISPUTE) trong vòng 48h sau khi nhận hàng
     *
     * Điều kiện auto xử lý:
     *  - status = SHIPPING
     *  - trackingStatus cho thấy là đã giao: "delivered" (tuỳ bạn map từ GHN)
     *  - updatedAt (hoặc deliveredAt) <= now - 48h
     *
     * Hành vi:
     *  - Đổi sang AUTO_REFUNDED (coi như hệ thống tự REFUND)
     *  - faultType:
     *      + Nếu reasonType = CUSTOMER_FAULT: vẫn set CUSTOMER
     *        → khách bị xem là lỗi trong việc phát sinh return, nên KHÔNG hoàn phí ship.
     *      + Nếu reasonType = SHOP_FAULT: set SHOP
     *  - Gọi walletService.refundForReturn(r):
     *      → chỉ hoàn tiền hàng (itemPrice), KHÔNG hoàn phí ship (shippingFee).
     */
    @Override
    @Transactional
    public void autoRefundForUnresponsiveShop() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);

        // Giả định findUnresponsiveReturns(status, deadline) lọc theo:
        //  - r.status = status
        //  - r.updatedAt <= deadline  (hoặc createdAt tuỳ bạn implement)
        List<ReturnRequest> list =
                returnRepo.findUnresponsiveReturns(ReturnStatus.SHIPPING, deadline);

        for (ReturnRequest r : list) {

            // ✅ Bảo vệ: chỉ auto refund khi hàng ĐÃ GIAO cho shop
            // Nếu bạn có mapping từ GHN thì chỉnh lại string này cho đúng.
            String tracking = r.getTrackingStatus();
            if (tracking == null || !tracking.equalsIgnoreCase("delivered")) {
                // chưa chắc đã nhận hàng → bỏ qua, chờ scheduler lần sau
                continue;
            }

            // ✅ Xác định faultType cuối cùng
            // Trường hợp bài toán "khách thật sự sai nhưng shop KHÔNG khiếu nại":
            //  - reasonType = CUSTOMER_FAULT
            //  - shop im lặng → hệ thống auto hoàn tiền hàng, NHƯNG:
            //      + giữ faultType = CUSTOMER để không hoàn phí ship
            ReturnFaultType finalFault =
                    (r.getReasonType() == ReturnReasonType.SHOP_FAULT)
                            ? ReturnFaultType.SHOP
                            : ReturnFaultType.CUSTOMER;   // mặc định CUSTOMER cho CUSTOMER_FAULT + không khiếu nại

            r.setStatus(ReturnStatus.AUTO_REFUNDED);
            r.setFaultType(finalFault);
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            // 💰 Hoàn tiền HÀNG cho customer, KHÔNG hoàn phí ship
            // refundForReturn chỉ dùng r.getItemPrice(), không dùng r.getShippingFee()
            refundAndDeductLegalPointIfNeeded(r);


            log.info(
                    "[AUTO REFUND RETURN][CASE 3] returnRequest={} auto-refunded after 48h without shop dispute, " +
                            "reasonType={}, faultType={}",
                    r.getId(), r.getReasonType(), finalFault
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnPreviewResponse previewReturnForOrder(UUID orderId) {

        UUID customerId = securityUtils.getCurrentCustomerId();

        // ✅ lấy thẳng items của order theo customerId (an toàn + đúng mục tiêu “hoàn theo sản phẩm”)
        List<CustomerOrderItem> items = customerOrderItemRepo
                .findAllByCustomerOrder_IdAndCustomerOrder_Customer_Id(orderId, customerId);

        if (items == null || items.isEmpty()) {
            throw new NoSuchElementException("Order not found or has no items");
        }

        var itemPreviews = items.stream().map(it -> {

            // ✅ Giá hoàn = số tiền thực trả sau mọi giảm giá (không tính ship)
            BigDecimal refundable = it.getAmountCharged();
            if (refundable == null) refundable = it.getFinalLineTotal();
            if (refundable == null) refundable = it.getLineTotal();
            if (refundable == null) refundable = BigDecimal.ZERO;

            return ReturnPreviewResponse.Item.builder()
                    .orderItemId(it.getId())
                    .productId(it.getRefId())
                    .productName(it.getName())
                    .quantity(it.getQuantity())
                    .refundableAmount(refundable)
                    .finalLineTotal(it.getFinalLineTotal())
                    .amountCharged(it.getAmountCharged())
                    .build();
        }).toList();

        return ReturnPreviewResponse.builder()
                .orderId(orderId)
                .items(itemPreviews)
                .build();
    }



    @Override
    @Transactional
    public void rejectReturnByShop(UUID returnRequestId, ReturnRejectRequest req) {
        UUID shopId = securityUtils.getCurrentStoreId();

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!r.getShopId().equals(shopId)) {
            throw new AccessDeniedException("Not your return request");
        }

        // Chỉ cho reject khi còn PENDING
        if (r.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("ReturnRequest must be PENDING to reject");
        }

        // Gắn trạng thái REJECTED
        r.setStatus(ReturnStatus.REJECTED);

        // Tuỳ bạn muốn gắn lỗi về phía ai, thường shop không chấp nhận → lỗi phía CUSTOMER
        r.setFaultType(ReturnFaultType.CUSTOMER);

        // Nếu muốn lưu lý do shop từ chối (nếu bạn có trường tương ứng trong entity)
        if (req != null && req.getShopRejectReason() != null) {
            r.setShopDisputeReason(req.getShopRejectReason()); // hoặc thêm field riêng như shopRejectReason
        }

        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);
    }

    /**
     * CASE 4.1 - Shop không phản hồi yêu cầu return trong 48h
     * Điều kiện:
     *  - status = PENDING
     *  - createdAt (hoặc updatedAt) <= now - 48h
     * Hành vi:
     *  - Đổi sang APPROVED giống như shop bấm approve
     */
    @Override
    @Transactional
    public void autoApprovePendingReturns() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);

        // Giả định findUnresponsiveReturns(status, deadline) lọc theo createdAt/updatedAt <= deadline
        List<ReturnRequest> list =
                returnRepo.findUnresponsiveReturns(ReturnStatus.PENDING, deadline);

        for (ReturnRequest r : list) {
            // Nếu sau 48h mà shop vẫn chưa reject → tự approve
            r.setStatus(ReturnStatus.APPROVED);
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            log.info("[AUTO APPROVE RETURN] returnRequest={} auto-approved after 48h pending", r.getId());
        }
    }

    /**
     * CASE 4.2 - Customer không gửi hàng sau khi shop approve (72h)
     * Điều kiện:
     *  - status = APPROVED
     *  - ghnOrderCode IS NULL (chưa tạo đơn GHN)
     *  - updatedAt (thời điểm approve) <= now - 72h
     * Hành vi:
     *  - Auto CANCEL return (trả về CANCELLED)
     *  - Nếu có logic "lock tiền cho shop" thì ở đây sẽ phải unlock lại (chưa làm trong hàm này)
     */
    @Override
    @Transactional
    public void autoCancelUnshippedReturns() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);

        List<ReturnRequest> list =
                returnRepo.findUnresponsiveReturns(ReturnStatus.APPROVED, deadline);

        for (ReturnRequest r : list) {
            // chỉ xử lý những request chưa từng tạo đơn GHN
            if (r.getGhnOrderCode() != null) continue;

            r.setStatus(ReturnStatus.CANCELLED);   // nhớ thêm vào enum
            r.setFaultType(ReturnFaultType.CUSTOMER); // khách không gửi hàng → xem như lỗi phía customer
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            log.info("[AUTO CANCEL RETURN] returnRequest={} auto-cancelled after 72h not shipped", r.getId());
        }
    }

    /**
     * CASE 4.4 - GHN không pickup sau 48h
     * Điều kiện:
     *  - status = SHIPPING
     *  - đã có ghnOrderCode
     *  - trackingStatus vẫn ở trạng thái chờ lấy (ready_to_pick / null)
     *  - updatedAt (hoặc createdAt GHN order) <= now - 48h
     * Hành vi:
     *  - (optional) gọi GHN hủy đơn cũ
     *  - clear ghnOrderCode + trackingStatus
     *  - đưa request về lại APPROVED để shop/customer tạo lệnh mới
     */
    @Override
    @Transactional
    public void autoHandleGhnPickupTimeout() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);
        List<ReturnRequest> list = returnRepo.findUnresponsiveReturns(ReturnStatus.SHIPPING, deadline);

        for (ReturnRequest r : list) {
            String tracking = r.getTrackingStatus();
            // Chỉ xử lý khi vẫn đang chờ lấy hàng (ready_to_pick, storing, hoặc null)
            if (tracking != null &&
                    !tracking.equalsIgnoreCase("ready_to_pick") &&
                    !tracking.equalsIgnoreCase("storing")) {
                continue;
            }

            String oldOrderCode = r.getGhnOrderCode();
            if (oldOrderCode == null || oldOrderCode.isBlank()) {
                continue;
            }

            // === GỌI API HỦY ĐƠN GHN THẬT SỰ ===
            try {
                String cancelUrl = "https://your-domain.com/api/ghn/cancel-order"; // Thay bằng domain thật của bạn
                // Hoặc nếu cùng service thì dùng RestTemplate inject vào

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // Token và ShopId sẽ được tự động thêm bởi GHNController → nhưng ở đây ta gửi thẳng body
                String body = "{\"order_codes\": [\"" + oldOrderCode + "\"]}";

                HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        cancelUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("[AUTO GHN CANCEL] SUCCESS - returnRequest={} cancelled GHN orderCode={}",
                            r.getId(), oldOrderCode);
                } else {
                    log.warn("[AUTO GHN CANCEL] API returned non-200 - returnRequest={}, orderCode={}, status={}, response={}",
                            r.getId(), oldOrderCode, response.getStatusCode(), response.getBody());
                }

            } catch (Exception ex) {
                log.error("[AUTO GHN CANCEL] FAILED - returnRequest={} cannot cancel GHN orderCode={}: {}",
                        r.getId(), oldOrderCode, ex.getMessage(), ex);
                // Không throw → vẫn tiếp tục reset về APPROVED dù hủy thất bại
            }

            // === Dù hủy thành công hay không, vẫn reset để shop tạo lại đơn mới ===
            r.setGhnOrderCode(null);
            r.setTrackingStatus(null);
            r.setStatus(ReturnStatus.APPROVED);
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            log.info("[AUTO GHN PICKUP TIMEOUT] returnRequest={} has been reset to APPROVED for re-creating GHN order",
                    r.getId());
        }
    }

    @Override
    @Transactional
    public ReturnRequestResponse shopConfirmReceivedAfterDelivered(UUID returnRequestId) {
        UUID shopId = securityUtils.getCurrentStoreId();

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!Objects.equals(r.getShopId(), shopId)) {
            throw new AccessDeniedException("Not your return request");
        }

        // Chỉ confirm khi đang SHIPPING (đang trả hàng về shop)
        if (r.getStatus() == ReturnStatus.DELIVERED) {
            throw new IllegalStateException("ReturnRequest must be DELIVERED to confirm");
        }

        // Chỉ confirm khi GHN đã giao trả về shop
        // tuỳ bạn map trackingStatus, ở code bạn đang dùng "delivered" cho auto refund
        ReturnStatus tracking = r.getStatus();
        if (!tracking.equals(ReturnStatus.SHIPPING)) {
            throw new IllegalStateException("GHN has not delivered return package to shop yet");
        }

        // Nếu admin đã phán SHOP thắng -> đóng luồng, không cho refund/confirm
        if (r.isFinalDecision()) {
            throw new IllegalStateException("Return request is closed by admin decision");
        }

        // Set trạng thái refunded
        r.setStatus(ReturnStatus.REFUNDED);
        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);

        // Refund tiền + trừ legal point nếu shop fault (reuse logic hiện có) :contentReference[oaicite:3]{index=3}
        refundAndDeductLegalPointIfNeeded(r);

        return toResponse(r);
    }


    @Override
    @Transactional
    public ReturnRequestResponse refundWithoutReturnByShop(UUID returnRequestId) {
        UUID shopId = securityUtils.getCurrentStoreId();

        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!r.getShopId().equals(shopId)) {
            throw new AccessDeniedException("Not your return request");
        }

        // Chỉ allow khi request vẫn đang PENDING và chưa có GHN
        if (r.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("ReturnRequest must be PENDING to refund without return");
        }
        if (r.getGhnOrderCode() != null) {
            throw new IllegalStateException("ReturnRequest already has GHN order, cannot refund-only");
        }

        // CASE 8: Refund Only
        // Mặc định đây là tình huống shop chấp nhận lỗi nhỏ / hỗ trợ khách
        // → nếu faultType chưa rõ thì set về SHOP cho rõ nghĩa
        if (r.getFaultType() == null || r.getFaultType() == ReturnFaultType.UNKNOWN) {
            r.setFaultType(ReturnFaultType.SHOP);
        }

        r.setStatus(ReturnStatus.REFUNDED);
        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);

        // 💰 Refund tiền hàng cho customer (itemPrice)
        // refundForReturn đã lo chuyện lấy từ platform pending → ví customer
        refundAndDeductLegalPointIfNeeded(r);


        log.info("[REFUND ONLY][SHOP] returnRequest={} refunded without physical return by shopId={}",
                r.getId(), shopId);

        return toResponse(r);
    }

    private void refundAndDeductLegalPointIfNeeded(ReturnRequest r) {

        // 1️⃣ Refund tiền – FAIL là throw → KHÔNG trừ điểm
        walletService.refundForReturn(r);

        // 2️⃣ Chỉ trừ điểm nếu lỗi SHOP
        if (r.getFaultType() != ReturnFaultType.SHOP) {
            return;
        }

        // 3️⃣ Chống trừ lặp
        if (Boolean.TRUE.equals(r.getLegalPointDeducted())) {
            return;
        }

        // 4️⃣ Trừ 1 legal point
        legalPointService.minusForStore(r.getShopId(), 1);

        // 5️⃣ Đánh dấu đã trừ
        r.setLegalPointDeducted(true);
        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);
    }

    @Transactional
    public void finalizeReturnShippingPayer(ReturnRequest r) {

        if (r.getFaultType() == null || r.getFaultType() == ReturnFaultType.UNKNOWN) return;

        ReturnShippingFee feeLog = shippingFeeRepo.findByReturnRequestId(r.getId()).orElse(null);
        if (feeLog == null) return;

        String finalPayer = payerFromFaultType(r.getFaultType());
        if (finalPayer == null) return;

        if (finalPayer.equalsIgnoreCase(feeLog.getPayer())) return;

        BigDecimal fee = feeLog.getShippingFee() == null ? BigDecimal.ZERO : feeLog.getShippingFee();

        feeLog.setPayer(finalPayer);

        if ("SHOP".equalsIgnoreCase(finalPayer)) {
            feeLog.setChargedToShop(fee);
            feeLog.setPaidByShop(true);
            feeLog.setShopFault(true);
        } else { // CUSTOMER
            feeLog.setChargedToShop(BigDecimal.ZERO);
            feeLog.setPaidByShop(false);
            feeLog.setShopFault(false);
        }

        shippingFeeRepo.save(feeLog);
    }



    //hepler
    private String payerFromReasonType(ReturnReasonType reasonType) {
        // CUSTOMER_FAULT => CUSTOMER chịu phí
        // SHOP_FAULT     => SHOP chịu phí
        return (reasonType == ReturnReasonType.CUSTOMER_FAULT) ? "CUSTOMER" : "SHOP";
    }

    private String payerFromFaultType(ReturnFaultType faultType) {
        // faultType = CUSTOMER => CUSTOMER chịu phí
        // faultType = SHOP     => SHOP chịu phí
        if (faultType == ReturnFaultType.SHOP) return "SHOP";
        if (faultType == ReturnFaultType.CUSTOMER) return "CUSTOMER";
        return null;
    }

    //helper admin tạo payer
    private ReturnShippingFee ensureReturnShippingFeeRow(ReturnRequest r) {
        return shippingFeeRepo.findByReturnRequestId(r.getId())
                .orElseGet(() -> shippingFeeRepo.save(
                        ReturnShippingFee.builder()
                                .returnRequestId(r.getId())
                                .storeId(r.getShopId())
                                .ghnOrderCode(null)              // chưa có
                                .shippingFee(BigDecimal.ZERO)    // ✅ vì chưa có phí
                                .payer("UNKNOWN")                   // sẽ set lại ngay dưới
                                .chargedToShop(BigDecimal.ZERO)
                                .shopFault(null)
                                .paidByShop(false)
                                .picked(false)
                                .build()
                ));
    }
    private void applyAdminPayerToReturnShippingFee(ReturnRequest r) {
        if (r.getFaultType() == null || r.getFaultType() == ReturnFaultType.UNKNOWN) return;

        ReturnShippingFee feeLog = ensureReturnShippingFeeRow(r);

        String payer = (r.getFaultType() == ReturnFaultType.SHOP) ? "SHOP" : "CUSTOMER";
        feeLog.setPayer(payer);

        BigDecimal fee = feeLog.getShippingFee() == null ? BigDecimal.ZERO : feeLog.getShippingFee();

        if ("SHOP".equalsIgnoreCase(payer)) {
            feeLog.setChargedToShop(fee);
            feeLog.setPaidByShop(true);
            feeLog.setShopFault(true);
        } else {
            feeLog.setChargedToShop(BigDecimal.ZERO);
            feeLog.setPaidByShop(false);
            feeLog.setShopFault(false);
        }

        shippingFeeRepo.save(feeLog);
    }
    //helper cus tạo package
    private void fillFeeWhenCustomerHasPackage(ReturnRequest r, BigDecimal fee) {
        ReturnShippingFee feeLog = ensureReturnShippingFeeRow(r);

        feeLog.setShippingFee(fee);

        // giữ payer hiện tại (nếu admin đã phán thì dùng payer đó)
        String payer = feeLog.getPayer();
        if ("SHOP".equalsIgnoreCase(payer)) {
            feeLog.setChargedToShop(fee);
            feeLog.setPaidByShop(false);
        } else {
            feeLog.setChargedToShop(BigDecimal.ZERO);
            feeLog.setPaidByShop(false);
        }

        shippingFeeRepo.save(feeLog);
    }

}
