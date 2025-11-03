package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.config.CodConfig;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.CartService;
import org.example.audio_ecommerce.service.GhnFeeService;
import static org.example.audio_ecommerce.service.Impl.GhnFeeRequestBuilder.buildForStoreShipment;
import org.example.audio_ecommerce.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final ProductComboRepository comboRepo;
    private final CustomerRepository customerRepo;
    private final WalletRepository walletRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final StoreRepository storeRepo;
    private final VoucherService voucherService;
    private final GhnFeeService ghnFeeService;

    // ====== NEW: để kiểm tra COD theo ví đặt cọc ======
    private final StoreWalletRepository storeWalletRepository;
    private final CodConfig codConfig;

    @Override
    @Transactional
    public CartResponse addItems(UUID customerId, AddCartItemsRequest request) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepo.save(Cart.builder().customer(customer).status(CartStatus.ACTIVE).build()));

        // Dùng key (type + refId) để merge
        Map<String, CartItem> existingMap = new HashMap<>();
        for (CartItem it : Optional.ofNullable(cart.getItems()).orElseGet(ArrayList::new)) {
            String key = key(it.getType(), it.getReferenceId());
            existingMap.put(key, it);
        }
        if (cart.getItems() == null) cart.setItems(new ArrayList<>());

        for (var line : request.getItems()) {
            CartItemType type = CartItemType.valueOf(line.getType().toUpperCase(Locale.ROOT));
            UUID refId = line.getId();
            int qty = Math.max(1, line.getQuantity());

            if (type == CartItemType.PRODUCT) {
                Product p = productRepo.findById(refId)
                        .orElseThrow(() -> new NoSuchElementException("Product not found: " + refId));

                // kiểm tồn đơn giản (nếu set)
                if (p.getStockQuantity() != null && p.getStockQuantity() < qty) {
                    throw new IllegalStateException("Product out of stock: " + p.getName());
                }

                BigDecimal unit = (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                        ? p.getDiscountPrice() : (p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);

                String k = key(type, p.getProductId());
                CartItem it = existingMap.get(k);
                if (it == null) {
                    it = CartItem.builder()
                            .cart(cart)
                            .type(type)
                            .product(p)
                            .quantity(qty)
                            .unitPrice(unit)
                            .lineTotal(unit.multiply(BigDecimal.valueOf(qty)))
                            .nameSnapshot(p.getName())
                            .imageSnapshot(firstImage(p.getImages()))
                            .build();
                    cart.getItems().add(it);
                    existingMap.put(k, it);
                } else {
                    it.setQuantity(it.getQuantity() + qty);
                    it.setUnitPrice(unit); // cập nhật theo giá hiện tại
                    it.setLineTotal(unit.multiply(BigDecimal.valueOf(it.getQuantity())));
                }
            } else {
                ProductCombo c = comboRepo.findById(refId)
                        .orElseThrow(() -> new NoSuchElementException("Combo not found: " + refId));

                if (c.getStockQuantity() != null && c.getStockQuantity() < qty) {
                    throw new IllegalStateException("Combo out of stock: " + c.getName());
                }

// ---- NEW: tính giá combo từ products ----
                BigDecimal comboUnitPrice = c.getItems().stream()
                        .map(ci -> {
                            Product p = ci.getProduct();
                            BigDecimal base = (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                                    ? p.getDiscountPrice()
                                    : p.getPrice();
                            return base.multiply(BigDecimal.valueOf(ci.getQuantity())); // quantity trong combo item
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);


                String k = key(type, c.getComboId());
                CartItem it = existingMap.get(k);
                if (it == null) {
                    it = CartItem.builder()
                            .cart(cart)
                            .type(type)
                            .combo(c)
                            .quantity(qty)
                            .unitPrice(comboUnitPrice)
                            .lineTotal(comboUnitPrice.multiply(BigDecimal.valueOf(qty)))
                            .nameSnapshot(c.getName())
                            .imageSnapshot(firstImage(c.getImages()))
                            .build();
                    cart.getItems().add(it);
                    existingMap.put(k, it);
                } else {
                    it.setQuantity(it.getQuantity() + qty);
                    it.setUnitPrice(comboUnitPrice);
                    it.setLineTotal(comboUnitPrice.multiply(BigDecimal.valueOf(it.getQuantity())));
                }
            }
        }

        recalcTotals(cart);
        cartRepo.save(cart);
        // Không cần save item riêng vì cascade ALL đã lo, nhưng giữ cho chắc:
        cartItemRepo.saveAll(cart.getItems());
        return toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getActiveCart(UUID customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseGet(() -> Cart.builder().customer(customer).status(CartStatus.ACTIVE).build());
        if (cart.getItems() == null) cart.setItems(new ArrayList<>());
        return toResponse(cart);
    }

    // ===== NEW: API để FE kiểm tra và khóa nút COD nếu cần =====
    @Override
    @Transactional(readOnly = true)
    public CodEligibilityResponse checkCodEligibility(UUID customerId, List<CheckoutItemRequest> reqItems) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));

        // 1) Map request -> CartItem trong cart
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : Optional.ofNullable(reqItems).orElse(List.of())) {
            CartItemType type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            UUID refId = req.getId();
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                    .findFirst()
                    .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for COD eligibility check");
        }

        // 2) Group theo store và tính subtotal từng store
        Map<UUID, StoreSubtotal> subtotalByStore = new HashMap<>();
        for (CartItem it : itemsToCheckout) {
            UUID storeId = (it.getType() == CartItemType.PRODUCT && it.getProduct() != null)
                    ? it.getProduct().getStore().getStoreId()
                    : (it.getCombo() != null ? it.getCombo().getStore().getStoreId() : null);
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");

            subtotalByStore.computeIfAbsent(storeId, k -> new StoreSubtotal())
                    .add(it.getLineTotal());

            // lưu name store (để trả ra FE)
            subtotalByStore.get(storeId).storeName =
                    (it.getType() == CartItemType.PRODUCT && it.getProduct() != null)
                            ? it.getProduct().getStore().getStoreName()
                            : (it.getCombo() != null ? it.getCombo().getStore().getStoreName() : null);
        }

        // 3) Tính requiredDeposit = subtotal * ratio, lấy depositBalance từ StoreWallet
        BigDecimal ratio = codConfig.getCodDepositRatio();
        List<CodEligibilityResponse.PerStore> perStores = new ArrayList<>();

        boolean overall = true;
        for (Map.Entry<UUID, StoreSubtotal> e : subtotalByStore.entrySet()) {
            UUID storeId = e.getKey();
            StoreSubtotal ss = e.getValue();

            BigDecimal required = ss.subtotal.multiply(ratio).setScale(0, java.math.RoundingMode.DOWN);
            BigDecimal deposit = storeWalletRepository.findByStore_StoreId(storeId)
                    .map(w -> w.getDepositBalance() == null ? BigDecimal.ZERO : w.getDepositBalance())
                    .orElse(BigDecimal.ZERO);

            boolean eligible = deposit.compareTo(required) >= 0;
            if (!eligible) overall = false;

            perStores.add(CodEligibilityResponse.PerStore.builder()
                    .storeId(storeId)
                    .storeName(ss.storeName)
                    .storeSubtotal(ss.subtotal)
                    .requiredDeposit(required)
                    .depositBalance(deposit)
                    .eligible(eligible)
                    .reason(eligible ? null : "INSUFFICIENT_DEPOSIT")
                    .build());
        }

        return CodEligibilityResponse.builder()
                .overallEligible(overall)
                .stores(perStores)
                .build();
    }

    @Override
    @Transactional
    public CustomerOrderResponse checkoutCODWithResponse(UUID customerId, CheckoutCODRequest request) {
        CustomerOrder customerOrder = createOrderInternal(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                false,                        // enforceCodDeposit = false (COD bỏ qua)
                request.getStoreVouchers(),
                request.getPlatformVouchers(),// truyền voucher theo shop
                request.getServiceTypeId()
        );

        // Re-fetch để chắc chắn các @PreUpdate đã chạy và totals đã tính
        customerOrder = customerOrderRepository.findById(customerOrder.getId()).orElse(customerOrder);

        // (Tuỳ chọn) Breakdown giảm theo từng shop cho FE (storeId -> discountTotal)
        var storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(customerOrder.getId());
        Map<UUID, BigDecimal> storeDiscounts = storeOrders.stream()
                .collect(Collectors.toMap(
                        so -> so.getStore().getStoreId(),
                        so -> Optional.ofNullable(so.getStoreVoucherDiscount()).orElse(BigDecimal.ZERO),
                        BigDecimal::add // phòng trùng khóa (hiếm)
                ));

        Map<String, BigDecimal> platformDiscountMap = new LinkedHashMap<>();
        try {
            if (customerOrder.getPlatformVoucherDetailJson() != null) {
                var node = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(customerOrder.getPlatformVoucherDetailJson());
                node.fields().forEachRemaining(e -> {
                    platformDiscountMap.put(e.getKey(), new BigDecimal(e.getValue().asText()));
                });
            }
        } catch (Exception ignored) {}

        CustomerOrderResponse resp = new CustomerOrderResponse();
        resp.setId(customerOrder.getId());
        resp.setStatus(customerOrder.getStatus().name());
        resp.setMessage(customerOrder.getMessage());
        resp.setCreatedAt(customerOrder.getCreatedAt().toString());

        // Trả đủ 3 con số quan trọng
        resp.setTotalAmount(Optional.ofNullable(customerOrder.getTotalAmount()).orElse(BigDecimal.ZERO));
        resp.setDiscountTotal(Optional.ofNullable(customerOrder.getDiscountTotal()).orElse(BigDecimal.ZERO));
        resp.setGrandTotal(Optional.ofNullable(customerOrder.getGrandTotal()).orElse(BigDecimal.ZERO));

        // (tuỳ chọn) gửi kèm breakdown theo shop
        resp.setStoreDiscounts(storeDiscounts);
        resp.setPlatformDiscount(platformDiscountMap);
        // Shipping snapshot
        resp.setReceiverName(customerOrder.getShipReceiverName());
        resp.setPhoneNumber(customerOrder.getShipPhoneNumber());
        resp.setCountry(customerOrder.getShipCountry());
        resp.setProvince(customerOrder.getShipProvince());
        resp.setDistrict(customerOrder.getShipDistrict());
        resp.setWard(customerOrder.getShipWard());
        resp.setStreet(customerOrder.getShipStreet());
        resp.setAddressLine(customerOrder.getShipAddressLine());
        resp.setPostalCode(customerOrder.getShipPostalCode());
        resp.setNote(customerOrder.getShipNote());

        return resp;
    }

    @Override
    @Transactional
    public CustomerOrder createOrderForOnline(UUID customerId, CheckoutCODRequest request) {
        return createOrderInternal(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                false,                         // online không check deposit
                request.getStoreVouchers(),
                request.getPlatformVouchers(),
                request.getServiceTypeId()// truyền voucher theo shop
        );
    }

    /* ================= helpers ================= */

    private static String key(CartItemType type, UUID refId) {
        return type.name() + ":" + refId;
    }

    private static String firstImage(List<String> images) {
        return (images != null && !images.isEmpty()) ? images.get(0) : null;
    }

    private static void recalcTotals(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cart.setDiscountTotal(BigDecimal.ZERO); // về sau có voucher thì cập nhật ở đây
        cart.setGrandTotal(subtotal.subtract(cart.getDiscountTotal()));
    }

    private static CartResponse toResponse(Cart cart) {
        var items = cart.getItems() == null ? List.<CartItem>of() : cart.getItems();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getId())
                .status(cart.getStatus().name())
                .subtotal(cart.getSubtotal())
                .discountTotal(cart.getDiscountTotal())
                .grandTotal(cart.getGrandTotal())
                .items(items.stream().map(ci -> CartResponse.Item.builder()
                        .cartItemId(ci.getCartItemId())
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .image(ci.getImageSnapshot())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .build()
                ).toList())
                .build();
    }

    @Transactional
    protected CustomerOrder createOrderInternal(
            UUID customerId,
            List<CheckoutItemRequest> itemsReq,
            UUID addressId,
            String message,
            boolean enforceCodDeposit,
            List<StoreVoucherUse> storeVouchers,
            List<PlatformVoucherUse> platformVouchers,
            Integer serviceTypeId
    ) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 1) Map request -> CartItem trong cart
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : Optional.ofNullable(itemsReq).orElse(List.of())) {
            CartItemType type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            UUID refId = req.getId();
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                    .findFirst()
                    .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }

        // 2) Gom theo store
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = (item.getType() == CartItemType.PRODUCT && item.getProduct() != null)
                    ? item.getProduct().getStore().getStoreId()
                    : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null);
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }

        // 2b) CHỈ COD mới check deposit (nhưng hiện đang bỏ cho COD theo yêu cầu)
        if (enforceCodDeposit) {
            BigDecimal ratio = codConfig.getCodDepositRatio();
            for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
                UUID storeIdKey = entry.getKey();
                BigDecimal storeSubtotal = entry.getValue().stream()
                        .map(CartItem::getLineTotal)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal required = storeSubtotal.multiply(ratio).setScale(0, java.math.RoundingMode.DOWN);
                BigDecimal deposit = storeWalletRepository.findByStore_StoreId(storeIdKey)
                        .map(w -> w.getDepositBalance() == null ? BigDecimal.ZERO : w.getDepositBalance())
                        .orElse(BigDecimal.ZERO);

                if (deposit.compareTo(required) < 0) {
                    throw new IllegalStateException(
                            "COD_DISABLED_DEPOSIT_INSUFFICIENT for store=" + storeIdKey
                                    + " required=" + required + " deposit=" + deposit);
                }
            }
        }

        // 3) Lấy địa chỉ
        CustomerAddress chosenAddr;
        if (addressId != null) {
            chosenAddr = customer.getAddresses().stream()
                    .filter(a -> a.getId().equals(addressId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Address not found"));
        } else {
            chosenAddr = customer.getAddresses().stream()
                    .filter(CustomerAddress::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No default address found for checkout"));
        }

        Integer toDistrictId = chosenAddr.getDistrictId();
        String toWardCode = chosenAddr.getWardCode();

        // 4) Tạo CustomerOrder + snapshot địa chỉ
        CustomerOrder customerOrder = CustomerOrder.builder()
                .customer(customer)
                .createdAt(java.time.LocalDateTime.now())
                .message(message)
                .status(OrderStatus.PENDING)
                .shipReceiverName(chosenAddr.getReceiverName())
                .shipPhoneNumber(chosenAddr.getPhoneNumber())
                .shipCountry(chosenAddr.getCountry())
                .shipProvince(chosenAddr.getProvince())
                .shipDistrict(chosenAddr.getDistrict())
                .shipWard(chosenAddr.getWard())
                .shipStreet(chosenAddr.getStreet())
                .shipAddressLine(chosenAddr.getAddressLine())
                .shipPostalCode(chosenAddr.getPostalCode())
                .shipNote(chosenAddr.getNote())
                .build();

        // 5) CustomerOrderItem
        List<CustomerOrderItem> customerOrderItems = new ArrayList<>();
        for (CartItem item : itemsToCheckout) {
            customerOrderItems.add(CustomerOrderItem.builder()
                    .customerOrder(customerOrder)
                    .type(item.getType().name())
                    .refId(item.getReferenceId())
                    .name(item.getNameSnapshot())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .storeId((item.getType() == CartItemType.PRODUCT && item.getProduct() != null)
                            ? item.getProduct().getStore().getStoreId()
                            : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null))
                    .build());
        }
        customerOrder.setItems(customerOrderItems);

        // 6) Lưu CustomerOrder
        customerOrder = customerOrderRepository.save(customerOrder);

        BigDecimal totalShipping = BigDecimal.ZERO;

        // 7) Tạo StoreOrders + chuẩn bị map items theo store để áp voucher
        Map<UUID, List<StoreOrderItem>> storeItemsMap = new HashMap<>();
        List<StoreOrder> persistedStoreOrders = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeIdKey = entry.getKey();
            Store store = storeRepo.findById(storeIdKey)
                    .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeIdKey));
            // ====== NEW: Tính phí ship GHN cho store này ======
            var reqGHN = buildForStoreShipment(
                    entry.getValue(),
                    toDistrictId,
                    toWardCode,
                    serviceTypeId // mặc định hàng nặng; FE có thể cho chọn 2/5 và truyền vào request checkout nếu muốn
            );
            String feeJson = ghnFeeService.calculateFeeRaw(reqGHN);
            BigDecimal shippingFee = extractTotalFee(feeJson); // xem hàm bên dưới
            totalShipping = totalShipping.add(shippingFee);

            StoreOrder storeOrder = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .customerOrder(customerOrder)
                    // snapshot địa chỉ:
                    .shipReceiverName(customerOrder.getShipReceiverName())
                    .shipPhoneNumber(customerOrder.getShipPhoneNumber())
                    .shipCountry(customerOrder.getShipCountry())
                    .shipProvince(customerOrder.getShipProvince())
                    .shipDistrict(customerOrder.getShipDistrict())
                    .shipWard(customerOrder.getShipWard())
                    .shipStreet(customerOrder.getShipStreet())
                    .shipAddressLine(customerOrder.getShipAddressLine())
                    .shipPostalCode(customerOrder.getShipPostalCode())
                    .shipNote(customerOrder.getShipNote())
                    // gán phí ship
                    .shippingFee(shippingFee)
                    .build();

            List<StoreOrderItem> storeOrderItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                storeOrderItems.add(StoreOrderItem.builder()
                        .storeOrder(storeOrder)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .build());
            }

            storeOrder.setItems(storeOrderItems);
            storeOrder = storeOrderRepository.save(storeOrder);
            persistedStoreOrders.add(storeOrder);
            storeItemsMap.put(storeOrder.getStore().getStoreId(), storeOrderItems);
        }

        // Lưu tổng phí ship ở CustomerOrder
        customerOrder.setShippingFeeTotal(totalShipping);
        customerOrder = customerOrderRepository.save(customerOrder);

        // 7b) Áp voucher theo shop (giữ nguyên)
        Map<UUID, BigDecimal> storeDiscountByStore =
                voucherService.computeDiscountByStore(storeVouchers, storeItemsMap);

        BigDecimal totalStoreDiscount = BigDecimal.ZERO;
        Map<UUID, String> storeVoucherDetailJsonMap = new HashMap<>();
        // nếu computeDiscountByStore của bạn đã có detail map, set vào; nếu chưa, bạn có thể generate theo input
        // ví dụ tạm: {"CODE":amount} từ storeDiscountByStore; tuỳ bạn đã có detail hơn trong service:
        for (StoreVoucherUse su : Optional.ofNullable(storeVouchers).orElse(List.of())) {
            // key theo store
            // ở đây giả sử StoreVoucherUse có storeId & code
            // tạo map json cho từng store dạng {"CODE":amount} — amount tạm thời set null, nếu cần chính xác thì bạn bổ sung service trả chi tiết
        }

        // set vào order theo storeDiscountByStore
        for (StoreOrder so : persistedStoreOrders) {
            BigDecimal d = storeDiscountByStore.getOrDefault(so.getStore().getStoreId(), BigDecimal.ZERO);
            if (d != null && d.signum() > 0) {
                so.setStoreVoucherDiscount(d);
                // optional: so.setStoreVoucherDetailJson(storeVoucherDetailJsonMap.get(so.getStore().getStoreId()));
                totalStoreDiscount = totalStoreDiscount.add(d);
                storeOrderRepository.save(so);
            }
        }

        // 7c) Áp voucher toàn sàn (mới)
        var platformResult = voucherService.computePlatformDiscounts(
                platformVouchers, storeItemsMap);

        BigDecimal totalPlatformDiscount = BigDecimal.ZERO;
        for (StoreOrder so : persistedStoreOrders) {
            BigDecimal d = platformResult.discountByStore
                    .getOrDefault(so.getStore().getStoreId(), BigDecimal.ZERO);
            if (d != null && d.signum() > 0) {
                so.setPlatformVoucherDiscount(d);
                so.setPlatformVoucherDetailJson(platformResult.toPlatformVoucherJson()); // lưu chung map vào từng store
                storeOrderRepository.save(so);
                totalPlatformDiscount = totalPlatformDiscount.add(d);
            }
        }

        // 7d) Cập nhật tổng discount + JSON lên CustomerOrder (trigger @PreUpdate => grandTotal)
        customerOrder.setStoreDiscountTotal(totalStoreDiscount);
        customerOrder.setPlatformDiscountTotal(totalPlatformDiscount);
        customerOrder.setPlatformVoucherDetailJson(platformResult.toPlatformVoucherJson());

        // Nếu bạn có JSON chi tiết cho store voucher toàn đơn:
        if (!storeVoucherDetailJsonMap.isEmpty()) {
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(storeVoucherDetailJsonMap);
                customerOrder.setStoreVoucherDetailJson(json);
            } catch (Exception ignored) {}
        }

        // 8) Xoá item khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);

        return customerOrder;
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID customerId, UpdateCartItemQtyRequest request) {
        if (request.getCartItemId() == null || request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("cartItemId & quantity >= 1 are required");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getCartItemId().equals(request.getCartItemId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cart item not found"));

        // kiểm tồn tùy theo type
        if (item.getType() == CartItemType.PRODUCT && item.getProduct() != null) {
            Integer stock = item.getProduct().getStockQuantity();
            if (stock != null && stock < request.getQuantity()) {
                throw new IllegalStateException("Product out of stock: " + item.getProduct().getName());
            }
            item.setQuantity(request.getQuantity());
            item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            // COMBO
            ProductCombo combo = item.getCombo();
            Integer stock = combo != null ? combo.getStockQuantity() : null;
            if (stock != null && stock < request.getQuantity()) {
                throw new IllegalStateException("Combo out of stock: " + (combo != null ? combo.getName() : ""));
            }
            item.setQuantity(request.getQuantity());
            item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        recalcTotals(cart);
        cartRepo.save(cart);
        cartItemRepo.save(item);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItems(UUID customerId, RemoveCartItemRequest request) {
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("cartItemIds is required");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));

        // lọc items cần xóa
        List<CartItem> toRemove = cart.getItems().stream()
                .filter(ci -> request.getCartItemIds().contains(ci.getCartItemId()))
                .toList();

        if (toRemove.isEmpty()) {
            // không tìm thấy, có thể trả luôn cart hiện tại
            return toResponse(cart);
        }

        cart.getItems().removeAll(toRemove);
        recalcTotals(cart);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(toRemove);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(UUID customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            List<CartItem> copy = new ArrayList<>(cart.getItems());
            cart.getItems().clear();
            recalcTotals(cart);
            cartRepo.save(cart);
            cartItemRepo.deleteAll(copy);
        } else {
            recalcTotals(cart);
            cartRepo.save(cart);
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse bulkUpdateQuantities(UUID customerId, BulkUpdateCartQtyRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("lines is required");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));

        Map<String, CartItem> map = new HashMap<>();
        for (CartItem it : Optional.ofNullable(cart.getItems()).orElseGet(ArrayList::new)) {
            map.put(it.getType().name() + ":" + it.getReferenceId(), it);
        }

        for (var line : request.getLines()) {
            if (line.getQuantity() == null || line.getQuantity() < 1) continue;

            CartItemType type = CartItemType.valueOf(line.getType().toUpperCase(Locale.ROOT));
            UUID refId = UUID.fromString(line.getRefId());
            String k = type.name() + ":" + refId;

            CartItem item = map.get(k);
            if (item == null) continue;

            // kiểm tồn
            if (type == CartItemType.PRODUCT && item.getProduct() != null) {
                Integer stock = item.getProduct().getStockQuantity();
                if (stock != null && stock < line.getQuantity()) {
                    throw new IllegalStateException("Product out of stock: " + item.getProduct().getName());
                }
            } else {
                ProductCombo combo = item.getCombo();
                Integer stock = combo != null ? combo.getStockQuantity() : null;
                if (stock != null && stock < line.getQuantity()) {
                    throw new IllegalStateException("Combo out of stock: " + (combo != null ? combo.getName() : ""));
                }
            }

            item.setQuantity(line.getQuantity());
            item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            cartItemRepo.save(item);
        }

        recalcTotals(cart);
        cartRepo.save(cart);
        return toResponse(cart);
    }


    // Tối giản: trích "data.total" từ JSON GHN
    private static BigDecimal extractTotalFee(String feeJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(feeJson);
            var total = node.path("data").path("total").asLong(0L);
            return BigDecimal.valueOf(total);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ===== helper class cho tính tổng theo store (chỉ dùng nội bộ) =====
    private static class StoreSubtotal {
        BigDecimal subtotal = BigDecimal.ZERO;
        String storeName;

        void add(BigDecimal v) {
            if (v != null) subtotal = subtotal.add(v);
        }
    }
}