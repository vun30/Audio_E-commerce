package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.ReturnPackageFeeResponse;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.ReturnFaultType;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.GhnFeeService;
import org.example.audio_ecommerce.service.GhnOrderService;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.example.audio_ecommerce.service.WalletService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

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

    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shopId}")
    private String ghnShopId;

    private static final String GHN_BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";

    // ================== Helper ==================

    private ReturnRequestResponse toResponse(ReturnRequest r) {
        return ReturnRequestResponse.builder()
                .id(r.getId())
                .customerId(r.getCustomerId())
                .shopId(r.getShopId())
                .orderItemId(r.getOrderItemId())
                .productId(r.getProductId())
                .productName(r.getProductName())
                .itemPrice(r.getItemPrice())
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
        BigDecimal itemPrice = orderItem.getUnitPrice(); // hoặc lineTotal nếu hoàn theo cả dòng

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
        if (customerOrder.getStatus() == OrderStatus.DELIVERY_SUCCESS) {
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

        return ReturnPackageFeeResponse.builder()
                .shippingFee(fee)
                .build();
    }

    // =========================================================
    // ======================== SHOP ===========================
    // =========================================================

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

        // Ai trả phí: CUSTOMER_FAULT → customer trả; ngược lại shop trả
        int paymentTypeId =
                (r.getReasonType() == ReturnReasonType.CUSTOMER_FAULT) ? 2 : 1;
        body.setPayment_type_id(paymentTypeId);

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

        // 6️⃣ Log phí ship
        ReturnShippingFee feeLog = ReturnShippingFee.builder()
                .returnRequestId(r.getId())
                .ghnOrderCode(orderCode)
                .shippingFee(r.getShippingFee() != null ? r.getShippingFee() : totalFee)
                .payer(r.getReasonType() == ReturnReasonType.CUSTOMER_FAULT ? "CUSTOMER" : "SHOP")
                .shopFault(r.getReasonType() == ReturnReasonType.SHOP_FAULT)
                .chargedToShop(r.getReasonType() == ReturnReasonType.SHOP_FAULT
                        ? (r.getShippingFee() != null ? r.getShippingFee() : totalFee)
                        : BigDecimal.ZERO)
                .picked(false)
                .build();
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
            walletService.refundForReturn(r);
        } else {
            // ✅ Shop khiếu nại: nói là hàng không đúng mô tả khách trả về
            r.setStatus(ReturnStatus.DISPUTE);
            r.setShopVideoUrl(req.getShopVideoUrl());
            r.setShopImageUrls(
                    Optional.ofNullable(req.getShopImageUrls())
                            .orElseGet(ArrayList::new)
            );
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

    @Override
    @Transactional
    public ReturnRequestResponse resolveDispute(
            UUID returnRequestId,
            ReturnDisputeResolveRequest req
    ) {
        ReturnRequest r = returnRepo.findById(returnRequestId)
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (r.getStatus() != ReturnStatus.DISPUTE) {
            throw new IllegalStateException("ReturnRequest must be DISPUTE");
        }

        r.setFaultType(req.getFaultType());

        if (Boolean.TRUE.equals(req.getRefundCustomer())) {
            r.setStatus(ReturnStatus.REFUNDED);
            walletService.refundForReturn(r);
        } else {
            r.setStatus(ReturnStatus.REJECTED);
        }

        r.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(r);
        return toResponse(r);
    }

    // =========================================================
    // ========================= AUTO ==========================
    // =========================================================

    @Override
    @Transactional
    public void autoRefundForUnresponsiveShop() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(3);
        List<ReturnRequest> list =
                returnRepo.findUnresponsiveReturns(ReturnStatus.SHIPPING, deadline);

        for (ReturnRequest r : list) {
            r.setStatus(ReturnStatus.AUTO_REFUNDED);
            r.setFaultType(
                    r.getReasonType() == ReturnReasonType.CUSTOMER_FAULT
                            ? ReturnFaultType.CUSTOMER
                            : ReturnFaultType.SHOP
            );
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            walletService.refundForReturn(r);
        }
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

}
