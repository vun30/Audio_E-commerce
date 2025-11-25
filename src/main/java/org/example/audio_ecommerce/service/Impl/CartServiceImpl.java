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

import org.example.audio_ecommerce.service.OrderCodeGeneratorService;
import org.example.audio_ecommerce.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final ProductVariantRepository productVariantRepo;
    private final OrderCodeGeneratorService orderCodeGeneratorService;
    private final PlatformCampaignProductRepository platformCampaignProductRepository;
    // ====== NEW: để kiểm tra COD theo ví đặt cọc ======
    private final StoreWalletRepository storeWalletRepository;
    private final CodConfig codConfig;

    @Override
    @Transactional
    public CartResponse addItems(UUID customerId, AddCartItemsRequest request) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));

        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepo.save(Cart.builder()
                        .customer(customer)
                        .status(CartStatus.ACTIVE)
                        .build()));

        // Map để merge các item trùng (type + refId)
        Map<String, CartItem> existingMap = new HashMap<>();
        for (CartItem it : Optional.ofNullable(cart.getItems()).orElseGet(ArrayList::new)) {
            String key = key(it.getType(), it.getReferenceId());
            existingMap.put(key, it);
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        for (var line : request.getItems()) {
            CartItemType type = CartItemType.valueOf(line.getType().toUpperCase(Locale.ROOT));
            int qty = Math.max(1, line.getQuantity());

            if (type == CartItemType.PRODUCT) {

                UUID productId = line.getProductId();
                UUID variantId = line.getVariantId();

                ProductVariantEntity variant = null;
                Product product = null;

                // Ưu tiên variantId: nếu có variantId thì tìm variant, lấy product từ đó
                if (variantId != null) {
                    variant = productVariantRepo.findById(variantId)
                            .orElseThrow(() -> new NoSuchElementException("Variant not found: " + variantId));
                    product = variant.getProduct();
                    if (product == null) {
                        throw new IllegalStateException("Variant has no product: " + variantId);
                    }
                    // nếu FE truyền cả productId thì validate cho chắc
                    if (productId != null && !product.getProductId().equals(productId)) {
                        throw new IllegalArgumentException("Variant not belong to product");
                    }
                } else {
                    // không có variant => bắt buộc phải có productId
                    if (productId == null) {
                        throw new IllegalArgumentException("Either productId or variantId must be provided for PRODUCT");
                    }
                    product = productRepo.findById(productId)
                            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
                }

                // check tồn kho
                if (variant != null) {
                    Integer vStock = variant.getVariantStock();
                    if (vStock != null && vStock < qty) {
                        throw new IllegalStateException("Variant out of stock: "
                                + variant.getOptionName() + " " + variant.getOptionValue());
                    }
                } else {
                    Integer pStock = product.getStockQuantity();
                    if (pStock != null && pStock < qty) {
                        throw new IllegalStateException("Product out of stock: " + product.getName());
                    }
                }

                UUID refId = product.getProductId();   // KEY chính cho PRODUCT
                UUID keyVariantId = (variant != null ? variant.getId() : null);
                String k = key(type, refId, keyVariantId);

                CartItem it = existingMap.get(k);

                if (it == null) {
                    int totalQty = qty;

                    BigDecimal unitPrice = resolveUnitPrice(product, variant, totalQty);

                    it = CartItem.builder()
                            .cart(cart)
                            .type(type)
                            .product(product)
                            .variant(variant)
                            .quantity(totalQty)
                            .unitPrice(unitPrice)
                            .lineTotal(unitPrice.multiply(BigDecimal.valueOf(totalQty)))
                            .nameSnapshot(product.getName())
                            .imageSnapshot(firstImage(product.getImages()))
                            .variantOptionNameSnapshot(variant != null ? variant.getOptionName() : null)
                            .variantOptionValueSnapshot(variant != null ? variant.getOptionValue() : null)
                            .build();

                    cart.getItems().add(it);
                    existingMap.put(k, it);
                } else {
                    int totalQty = it.getQuantity() + qty;

                    BigDecimal unitPrice = resolveUnitPrice(product, variant, totalQty);

                    it.setQuantity(totalQty);
                    it.setUnitPrice(unitPrice);
                    it.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(totalQty)));
                }

            } else if (type == CartItemType.COMBO) {
                UUID comboId = line.getComboId();
                if (comboId == null) {
                    // fallback: nếu bạn muốn dùng field cũ line.getId() thì có thể thêm vào
                    throw new IllegalArgumentException("comboId is required for COMBO");
                }

                ProductCombo c = comboRepo.findById(comboId)
                        .orElseThrow(() -> new NoSuchElementException("Combo not found: " + comboId));

                if (c.getStockQuantity() != null && c.getStockQuantity() < qty) {
                    throw new IllegalStateException("Combo out of stock: " + c.getName());
                }

                // Tính giá combo
                BigDecimal comboUnitPrice = c.getItems().stream()
                        .map(ci -> {
                            Product cp = ci.getProduct();
                            BigDecimal base = (cp.getDiscountPrice() != null && cp.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                                    ? cp.getDiscountPrice()
                                    : cp.getPrice();
                            return base.multiply(BigDecimal.valueOf(ci.getQuantity()));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                UUID refId = c.getComboId();
                String k = key(type, refId, null);

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
                    int newQty = it.getQuantity() + qty;
                    it.setQuantity(newQty);
                    it.setUnitPrice(comboUnitPrice);
                    it.setLineTotal(comboUnitPrice.multiply(BigDecimal.valueOf(newQty)));
                }
            }
        }

        recalcTotals(cart);
        cartRepo.save(cart);
        // cascade ALL nên không cần save riêng items, nhưng giữ lại nếu muốn chắc chắn
        // cartItemRepo.saveAll(cart.getItems());

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
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && matchesCartItem(it, req))
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
    public List<CustomerOrderResponse> checkoutCODWithResponse(UUID customerId, CheckoutCODRequest request) {
        List<CustomerOrder> customerOrder = createOrdersSplitByStore(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                PaymentMethod.COD,                         // enforceCodDeposit = false (COD bỏ qua)
                request.getStoreVouchers(),
                request.getPlatformVouchers(),// truyền voucher theo shop
                request.getServiceTypeIds()
        );
        return customerOrder.stream().map(this::toOrderResponse).toList();
    }

    @Override
    @Transactional
    public List<CustomerOrderResponse> createOrderForOnline(UUID customerId, CheckoutCODRequest request) {
        List<CustomerOrder> orders = createOrdersSplitByStore(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                PaymentMethod.ONLINE, // online không check deposit
                request.getStoreVouchers(),
                request.getPlatformVouchers(),
                request.getServiceTypeIds()
        );
        return orders.stream().map(this::toOrderResponse).toList();
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

        List<CartResponse.Item> itemDtos = items.stream().map(ci -> {
            String type = ci.getType().name();
            UUID refId = ci.getReferenceId();

            String originProvince = null, originDistrict = null, originWard = null;

            if (ci.getType() == CartItemType.PRODUCT && ci.getProduct() != null) {
                Product p = ci.getProduct();
                originProvince = p.getProvinceCode();
                originDistrict = p.getDistrictCode();
                originWard = p.getWardCode();
            } else if (ci.getType() == CartItemType.COMBO && ci.getCombo() != null) {
                // Lấy mã origin từ 1 sản phẩm bất kỳ trong combo (ưu tiên cái có đủ code)
                ProductCombo combo = ci.getCombo();
                if (combo.getItems() != null) {
                    for (var citem : combo.getItems()) {
                        Product p = citem.getProduct();
                        if (p != null && (p.getProvinceCode() != null || p.getDistrictCode() != null || p.getWardCode() != null)) {
                            originProvince = p.getProvinceCode();
                            originDistrict = p.getDistrictCode();
                            originWard = p.getWardCode();
                            break;
                        }
                    }
                }
                // Nếu không tìm được thì để null (FE tự xử lý hiển thị)
            }

            // ✅ Lấy thông tin variant từ CartItem
            UUID variantId = ci.getVariantIdOrNull();                    // helper bạn đã có
            String variantOptionName = ci.getVariantOptionNameSnapshot();
            String variantOptionValue = ci.getVariantOptionValueSnapshot();
            String variantUrl = null;
            if (ci.getVariant() != null) {
                variantUrl = ci.getVariant().getVariantUrl();
            }

            return CartResponse.Item.builder()
                    .cartItemId(ci.getCartItemId())
                    .type(type)
                    .refId(refId)
                    .name(ci.getNameSnapshot())
                    .image(ci.getImageSnapshot())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .lineTotal(ci.getLineTotal())
                    .originProvinceCode(originProvince)
                    .originDistrictCode(originDistrict)
                    .originWardCode(originWard)
                    .variantId(variantId)
                    .variantOptionName(variantOptionName)
                    .variantOptionValue(variantOptionValue)
                    .variantUrl(variantUrl)
                    .build();
        }).toList();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getId())
                .status(cart.getStatus().name())
                .subtotal(cart.getSubtotal())
                .discountTotal(cart.getDiscountTotal())
                .grandTotal(cart.getGrandTotal())
                .items(itemDtos)
                .build();
    }


    @Transactional
    protected List<CustomerOrder> createOrdersSplitByStore(
            UUID customerId,
            List<CheckoutItemRequest> itemsReq,
            UUID addressId,
            String message,
            PaymentMethod paymentMethod,
            List<StoreVoucherUse> storeVouchers,
            List<PlatformVoucherUse> platformVouchers,
            Map<UUID, Integer> serviceTypeIds
    ) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 1) Map request -> CartItem
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : Optional.ofNullable(itemsReq).orElse(List.of())) {
            CartItemType type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && matchesCartItem(it, req))
                    .findFirst()
                    .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }

        // ✅ Trừ tồn kho theo items chuẩn bị checkout
        deductStockForCartItems(itemsToCheckout);

        // 2) Group theo store
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = (item.getType() == CartItemType.PRODUCT && item.getProduct() != null)
                    ? item.getProduct().getStore().getStoreId()
                    : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null);
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }

        // 3) Lấy địa chỉ
        CustomerAddress addr;
        if (addressId != null) {
            addr = customer.getAddresses().stream()
                    .filter(a -> a.getId().equals(addressId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Address not found"));
        } else {
            addr = customer.getAddresses().stream()
                    .filter(CustomerAddress::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No default address found for checkout"));
        }

        Integer toDistrictId = addr.getDistrictId();
        String toWardCode = addr.getWardCode();

        if (toDistrictId == null || toDistrictId <= 0 || toWardCode == null || toWardCode.isBlank()) {
            throw new IllegalStateException(
                    "Checkout address missing districtId/wardCode for GHN fee (addressId=" + addr.getId() + ")"
            );
        }

        // Dùng cho voucher services
        Map<UUID, List<StoreOrderItem>> storeItemsMap = new HashMap<>();
        Map<UUID, Store> storeCache = new HashMap<>();

        // Kết quả orders để trả về
        List<CustomerOrder> createdOrders = new ArrayList<>();

        // 4) Loop từng shop → tạo 1 CustomerOrder riêng
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeIdKey = entry.getKey();
            Store store = storeRepo.findById(storeIdKey)
                    .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeIdKey));
            storeCache.put(storeIdKey, store);

            String orderCode = orderCodeGeneratorService.nextOrderCode();
        // 🔹 Lấy địa chỉ origin của shop
            StoreAddressEntity originAddr = resolveStoreOriginAddress(store);
            String fromDistrictCode = originAddr != null ? originAddr.getDistrictCode() : null;
            String fromWardCode = originAddr != null ? originAddr.getWardCode() : null;

        // 4a) Tính phí GHN cho shop này
            Integer serviceTypeIdForStore = Optional.ofNullable(serviceTypeIds)
                    .map(m -> m.get(storeIdKey))
                    .orElse(5);

            var reqGHN = buildForStoreShipment(
                    entry.getValue(),
                    toDistrictId,          // Integer
                    toWardCode,
                    fromDistrictCode,      // String
                    fromWardCode,          // String// String
                    serviceTypeIdForStore  // Integer
            );


            // === LOG REQUEST JSON ===
            try {
                String jsonReq = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(reqGHN);
                log.info("[GHN-FEE][STORE:{}][CO?] Request payload:\n{}", storeIdKey, jsonReq);
            } catch (Exception e) {
                log.warn("[GHN-FEE] Failed to serialize request payload", e);
            }
            // Call service
            String feeRaw = ghnFeeService.calculateFeeRaw(reqGHN);

            // === LOG RESPONSE RAW ===
            log.info("[GHN-FEE][STORE:{}] Response raw: {}", storeIdKey, feeRaw);

            BigDecimal shippingFee = extractTotalFee(ghnFeeService.calculateFeeRaw(reqGHN));

            // 4b) Tạo CustomerOrder cho shop
            CustomerOrder co = CustomerOrder.builder()
                    .customer(customer)
                    .createdAt(java.time.LocalDateTime.now())
                    .message(message)
                    .status(OrderStatus.PENDING)
                    .orderCode(orderCode)
                    // snapshot địa chỉ
                    .shipReceiverName(addr.getReceiverName())
                    .shipPhoneNumber(addr.getPhoneNumber())
                    .shipCountry(addr.getCountry())
                    .shipProvince(addr.getProvince())
                    .shipDistrict(addr.getDistrict())
                    .shipWard(addr.getWard())
                    .shipStreet(addr.getStreet())
                    .shipAddressLine(addr.getAddressLine())
                    .shipPostalCode(addr.getPostalCode())
                    .shipNote(addr.getNote())
                    .build();

            co.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.ONLINE);

            // 4c) Items của riêng shop này
            List<CustomerOrderItem> coItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                coItems.add(CustomerOrderItem.builder()
                        .customerOrder(co)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .storeId(storeIdKey)
                        .build());
            }
            co.setItems(coItems);

            // 4d) Subtotal
            BigDecimal subtotal = coItems.stream()
                    .map(CustomerOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // set tạm
            co.setTotalAmount(subtotal);
            co.setShippingFeeTotal(shippingFee);

            // Lưu để có id
            co = customerOrderRepository.save(co);

            // 4e) Tạo StoreOrder (liên kết đến co)
            StoreOrder so = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .customerOrder(co)
                    .orderCode(orderCode)
                    // snapshot địa chỉ:
                    .shipReceiverName(co.getShipReceiverName())
                    .shipPhoneNumber(co.getShipPhoneNumber())
                    .shipCountry(co.getShipCountry())
                    .shipProvince(co.getShipProvince())
                    .shipDistrict(co.getShipDistrict())
                    .shipWard(co.getShipWard())
                    .shipStreet(co.getShipStreet())
                    .shipAddressLine(co.getShipAddressLine())
                    .shipPostalCode(co.getShipPostalCode())
                    .shipNote(co.getShipNote())
                    .shippingFee(shippingFee)
                    .shippingServiceTypeId(serviceTypeIdForStore)
                    .build();
            so.setPaymentMethod(co.getPaymentMethod());


            List<StoreOrderItem> soItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                soItems.add(StoreOrderItem.builder()
                        .storeOrder(so)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .build());
            }
            so.setItems(soItems);
            storeOrderRepository.save(so);

            // gom cho voucher service
            storeItemsMap.put(storeIdKey, soItems);

            createdOrders.add(co);
        }

        // 5) Áp voucher theo shop + platform cho từng shop
        var storeResult = voucherService.computeDiscountByStoreWithDetail(storeVouchers, storeItemsMap);
        var platformResult = voucherService.computePlatformDiscounts(platformVouchers, storeItemsMap);
        Map<UUID, String> storeDetailJsonByStore = storeResult.toDetailJsonByStore();
        Map<UUID, String> platformDetailJsonByStore = platformResult.toPerStoreJson();

        // 6) Cập nhật từng CustomerOrder: discount/grand + JSON detail
        for (CustomerOrder co : createdOrders) {
            UUID storeIdOfOrder = co.getItems().stream()
                    .map(CustomerOrderItem::getStoreId)
                    .findFirst().orElse(null);

            BigDecimal storeDiscount = storeResult.discountByStore.getOrDefault(storeIdOfOrder, BigDecimal.ZERO);
            BigDecimal platformDiscount = platformResult.discountByStore.getOrDefault(storeIdOfOrder, BigDecimal.ZERO);
            BigDecimal discountTotal = storeDiscount.add(platformDiscount);

            BigDecimal grand = co.getTotalAmount()
                    .add(co.getShippingFeeTotal())
                    .subtract(discountTotal);

            // set vào order
            co.setStoreDiscountTotal(storeDiscount);
            co.setPlatformDiscountTotal(platformDiscount);
            co.setDiscountTotal(discountTotal);
            co.setGrandTotal(grand);
            co.setPlatformVoucherDetailJson(platformResult.toPlatformVoucherJson());
            // nếu bạn có JSON chi tiết cho store-voucher, set vào co.setStoreVoucherDetailJson(...)

            customerOrderRepository.save(co);
        }

        // === NEW: đổ voucher xuống từng StoreOrder (GHN) ===
        for (CustomerOrder co : createdOrders) {
            List<StoreOrder> sos = storeOrderRepository.findAllByCustomerOrder_Id(co.getId());
            if (sos == null || sos.isEmpty()) continue;

            for (StoreOrder so : sos) {
                UUID sid = so.getStore().getStoreId();
                BigDecimal sv = storeResult.discountByStore.getOrDefault(sid, BigDecimal.ZERO);
                BigDecimal pv = platformResult.discountByStore.getOrDefault(sid, BigDecimal.ZERO);

                so.setStoreVoucherDiscount(sv);
                so.setPlatformVoucherDiscount(pv);

                // JSON chi tiết theo mã (shop) & platform
                String storeJson = storeDetailJsonByStore.getOrDefault(sid, "{}");
                String platJson = platformDetailJsonByStore.getOrDefault(sid, "{}");
                so.setStoreVoucherDetailJson(storeJson);
                so.setPlatformVoucherDetailJson(platJson);

                storeOrderRepository.save(so);
            }
        }

        // 7) Xoá item khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);

        return createdOrders;
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
            Product p = item.getProduct();
            ProductVariantEntity v = item.getVariant();

            Integer stock;
            if (v != null) {
                stock = v.getVariantStock();
            } else {
                stock = p.getStockQuantity();
            }

            if (stock != null && stock < request.getQuantity()) {
                throw new IllegalStateException("Product/Variant out of stock: " + p.getName());
            }

            int q = request.getQuantity();
            item.setQuantity(q);

            BigDecimal unit = resolveUnitPrice(p, v, q);

            item.setUnitPrice(unit);
            item.setLineTotal(unit.multiply(BigDecimal.valueOf(q)));

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
            int q = line.getQuantity();

            if (type == CartItemType.PRODUCT && item.getProduct() != null) {
                Product p = item.getProduct();
                Integer stock = p.getStockQuantity();
                if (stock != null && stock < q) {
                    throw new IllegalStateException("Product out of stock: " + p.getName());
                }

                item.setQuantity(q);
                BigDecimal unit = getUnitPriceWithBulk(p, q);
                item.setUnitPrice(unit);
                item.setLineTotal(unit.multiply(BigDecimal.valueOf(q)));
            } else {
                // COMBO
                ProductCombo combo = item.getCombo();
                Integer stock = combo != null ? combo.getStockQuantity() : null;
                if (stock != null && stock < q) {
                    throw new IllegalStateException("Combo out of stock: " + (combo != null ? combo.getName() : ""));
                }

                item.setQuantity(q);
                item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            cartItemRepo.save(item);

        }

        recalcTotals(cart);
        cartRepo.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public List<CustomerOrderResponse> checkoutStoreShip(UUID customerId, CheckoutCODRequest request) {
        // Giống online/COD nhưng:
        // - KHÔNG gọi GHN
        // - phí ship = 0 cho từng store
        // - shippingServiceTypeId = null
        // - có thể đặt PaymentMethod tùy: COD hay ONLINE (ở đây mình để theo request.paymentMethod nếu bạn có,
        //   còn nếu chưa có trong request thì mặc định COD cho store-ship)
        List<CustomerOrder> orders = createOrdersSplitByStore_StoreShipNoFee(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                // store-ship không check deposit COD (thường không cần),
                // nếu bạn muốn vẫn check thì set true
                false,
                request.getStoreVouchers(),
                request.getPlatformVouchers()
        );
        return orders.stream().map(this::toOrderResponse).toList();
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

    private CustomerOrderResponse toOrderResponse(CustomerOrder order) {
        CustomerOrderResponse resp = new CustomerOrderResponse();
        resp.setId(order.getId());
        resp.setOrderCode(order.getOrderCode());
        resp.setStatus(order.getStatus().name());
        resp.setMessage(order.getMessage());
        resp.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        // Lấy storeId/storeName từ StoreOrder của order (1 shop/1 order)
        var storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(order.getId());
        UUID storeId = null;
        String storeName = null;
        Integer svTypeId = null;
        BigDecimal storeVoucherDiscount = BigDecimal.ZERO;
        BigDecimal platformVoucherDiscount = BigDecimal.ZERO;

        if (!storeOrders.isEmpty()) {
            StoreOrder so = storeOrders.get(0);
            storeId = so.getStore().getStoreId();
            storeName = so.getStore().getStoreName();
            svTypeId = so.getShippingServiceTypeId();
            storeVoucherDiscount = Optional.ofNullable(so.getStoreVoucherDiscount()).orElse(BigDecimal.ZERO);
            platformVoucherDiscount = Optional.ofNullable(so.getPlatformVoucherDiscount()).orElse(BigDecimal.ZERO);
        }
        resp.setStoreId(storeId);
        resp.setStoreName(storeName);
        resp.setShippingServiceTypeId(svTypeId);

        // Tổng số
        resp.setTotalAmount(Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO));
        resp.setShippingFeeTotal(Optional.ofNullable(order.getShippingFeeTotal()).orElse(BigDecimal.ZERO));

        // discountTotal của riêng shop này
        BigDecimal discountTotal = Optional.ofNullable(order.getDiscountTotal()).orElse(
                storeVoucherDiscount.add(platformVoucherDiscount)
        );
        resp.setDiscountTotal(discountTotal);

        resp.setGrandTotal(Optional.ofNullable(order.getGrandTotal())
                .orElse(resp.getTotalAmount().add(resp.getShippingFeeTotal()).subtract(discountTotal)));

        // Map detail platformDiscount: parse JSON rồi lọc phần số tiền (nếu JSON không chia theo shop thì trả nguyên map)
        Map<String, BigDecimal> platformDiscountMap = new LinkedHashMap<>();
        try {
            if (order.getPlatformVoucherDetailJson() != null) {
                var node = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(order.getPlatformVoucherDetailJson());
                node.fields().forEachRemaining(e -> {
                    platformDiscountMap.put(e.getKey(), new BigDecimal(e.getValue().asText("0")));
                });
            }
        } catch (Exception ignore) {
        }
        resp.setPlatformDiscount(platformDiscountMap);

        // Nếu bạn đã lưu JSON chi tiết cho store-voucher per order, parse vào resp.setStoreVoucherDiscount(map)
        // Nếu chưa có detail theo mã, có thể set null hoặc map rỗng.

        // Shipping snapshot
        resp.setReceiverName(order.getShipReceiverName());
        resp.setPhoneNumber(order.getShipPhoneNumber());
        resp.setCountry(order.getShipCountry());
        resp.setProvince(order.getShipProvince());
        resp.setDistrict(order.getShipDistrict());
        resp.setWard(order.getShipWard());
        resp.setStreet(order.getShipStreet());
        resp.setAddressLine(order.getShipAddressLine());
        resp.setPostalCode(order.getShipPostalCode());
        resp.setNote(order.getShipNote());

        return resp;
    }

    @Transactional
    protected List<CustomerOrder> createOrdersSplitByStore_StoreShipNoFee(
            UUID customerId,
            List<CheckoutItemRequest> itemsReq,
            UUID addressId,
            String message,
            boolean enforceCodDeposit,
            List<StoreVoucherUse> storeVouchers,
            List<PlatformVoucherUse> platformVouchers
    ) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 1) Map request -> CartItem
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : Optional.ofNullable(itemsReq).orElse(List.of())) {
            CartItemType type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && matchesCartItem(it, req))
                    .findFirst()
                    .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }
        deductStockForCartItems(itemsToCheckout);

        // 2) Group theo store
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = (item.getType() == CartItemType.PRODUCT && item.getProduct() != null)
                    ? item.getProduct().getStore().getStoreId()
                    : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null);
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }

        // 2b) (tùy chọn) enforce COD deposit theo shop
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
        CustomerAddress addr;
        if (addressId != null) {
            addr = customer.getAddresses().stream()
                    .filter(a -> a.getId().equals(addressId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Address not found"));
        } else {
            addr = customer.getAddresses().stream()
                    .filter(CustomerAddress::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No default address found for checkout"));
        }

        // Dùng cho voucher services
        Map<UUID, List<StoreOrderItem>> storeItemsMap = new HashMap<>();

        // Kết quả
        List<CustomerOrder> createdOrders = new ArrayList<>();

        // 4) Loop từng shop → tạo 1 CustomerOrder riêng
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeIdKey = entry.getKey();
            Store store = storeRepo.findById(storeIdKey)
                    .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeIdKey));

            // === KHÁC BIỆT: phí ship = 0, không gọi GHN
            BigDecimal shippingFee = BigDecimal.ZERO;
            Integer serviceTypeIdForStore = null; // không dùng

            String orderCode = orderCodeGeneratorService.nextOrderCode();

            CustomerOrder co = CustomerOrder.builder()
                    .customer(customer)
                    .createdAt(java.time.LocalDateTime.now())
                    .message(message)
                    .status(OrderStatus.PENDING)
                    .orderCode(orderCode)
                    // snapshot địa chỉ
                    .shipReceiverName(addr.getReceiverName())
                    .shipPhoneNumber(addr.getPhoneNumber())
                    .shipCountry(addr.getCountry())
                    .shipProvince(addr.getProvince())
                    .shipDistrict(addr.getDistrict())
                    .shipWard(addr.getWard())
                    .shipStreet(addr.getStreet())
                    .shipAddressLine(addr.getAddressLine())
                    .shipPostalCode(addr.getPostalCode())
                    .shipNote(addr.getNote())
                    .build();

            // Bạn muốn mặc định COD cho store-ship? (đổi nếu cần)
            co.setPaymentMethod(PaymentMethod.STORE);

            // Items
            List<CustomerOrderItem> coItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                coItems.add(CustomerOrderItem.builder()
                        .customerOrder(co)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .storeId(storeIdKey)
                        .build());
            }
            co.setItems(coItems);

            BigDecimal subtotal = coItems.stream()
                    .map(CustomerOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            co.setTotalAmount(subtotal);
            co.setShippingFeeTotal(shippingFee);

            co = customerOrderRepository.save(co);

            // StoreOrder
            StoreOrder so = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .customerOrder(co)
                    .orderCode(orderCode)
                    .shipReceiverName(co.getShipReceiverName())
                    .shipPhoneNumber(co.getShipPhoneNumber())
                    .shipCountry(co.getShipCountry())
                    .shipProvince(co.getShipProvince())
                    .shipDistrict(co.getShipDistrict())
                    .shipWard(co.getShipWard())
                    .shipStreet(co.getShipStreet())
                    .shipAddressLine(co.getShipAddressLine())
                    .shipPostalCode(co.getShipPostalCode())
                    // ghi chú rõ để FE phân biệt
                    .shipNote((co.getShipNote() == null ? "" : co.getShipNote() + " | ") + "[STORE_SHIP - FREE]")
                    .shippingFee(shippingFee)
                    .shippingServiceTypeId(serviceTypeIdForStore) // null
                    .build();
            so.setPaymentMethod(co.getPaymentMethod());

            List<StoreOrderItem> soItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                soItems.add(StoreOrderItem.builder()
                        .storeOrder(so)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        .build());
            }
            so.setItems(soItems);
            storeOrderRepository.save(so);

            storeItemsMap.put(storeIdKey, soItems);
            createdOrders.add(co);
        }

        // 5) Áp voucher như bình thường (không ảnh hưởng phí ship vì = 0)
        var storeResult = voucherService.computeDiscountByStoreWithDetail(storeVouchers, storeItemsMap);
        var platformResult = voucherService.computePlatformDiscounts(platformVouchers, storeItemsMap);
        Map<UUID, String> storeDetailJsonByStore = storeResult.toDetailJsonByStore();
        Map<UUID, String> platformDetailJsonByStore = platformResult.toPerStoreJson();

        // 6) Cập nhật discount + grand
        for (CustomerOrder co : createdOrders) {
            UUID storeIdOfOrder = co.getItems().stream()
                    .map(CustomerOrderItem::getStoreId)
                    .findFirst().orElse(null);

            BigDecimal storeDiscount = storeResult.discountByStore.getOrDefault(storeIdOfOrder, BigDecimal.ZERO);
            BigDecimal platformDiscount = platformResult.discountByStore.getOrDefault(storeIdOfOrder, BigDecimal.ZERO);
            BigDecimal discountTotal = storeDiscount.add(platformDiscount);

            BigDecimal grand = co.getTotalAmount()
                    .add(co.getShippingFeeTotal()) // = 0
                    .subtract(discountTotal);

            co.setStoreDiscountTotal(storeDiscount);
            co.setPlatformDiscountTotal(platformDiscount);
            co.setDiscountTotal(discountTotal);
            co.setGrandTotal(grand);
            co.setPlatformVoucherDetailJson(platformResult.toPlatformVoucherJson());

            customerOrderRepository.save(co);
        }

        // === NEW: đổ voucher xuống từng StoreOrder (Store-Ship) ===
        for (CustomerOrder co : createdOrders) {
            List<StoreOrder> sos = storeOrderRepository.findAllByCustomerOrder_Id(co.getId());
            if (sos == null || sos.isEmpty()) continue;

            for (StoreOrder so : sos) {
                UUID sid = so.getStore().getStoreId();
                BigDecimal sv = storeResult.discountByStore.getOrDefault(sid, BigDecimal.ZERO);
                BigDecimal pv = platformResult.discountByStore.getOrDefault(sid, BigDecimal.ZERO);

                so.setStoreVoucherDiscount(sv);
                so.setPlatformVoucherDiscount(pv);

                String storeJson = storeDetailJsonByStore.getOrDefault(sid, "{}");
                String platJson = platformDetailJsonByStore.getOrDefault(sid, "{}");
                so.setStoreVoucherDetailJson(storeJson);
                so.setPlatformVoucherDetailJson(platJson);

                storeOrderRepository.save(so);
            }
        }

        // 7) Xóa items khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);

        return createdOrders;
    }

    // ================= BULK DISCOUNT HELPERS =================

    /** Giá base của product: ưu tiên discountPrice nếu > 0, fallback sang price. */
    private BigDecimal getBaseUnitPrice(Product p) {
        if (p == null) return BigDecimal.ZERO;
        if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
            return p.getDiscountPrice();
        }
        if (p.getPrice() != null) {
            return p.getPrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Áp bulk discount cho product theo tổng quantity.
     * Nếu quantity nằm trong bất kỳ khoảng [from, to] thì dùng unitPrice của khoảng đó.
     * Nếu không, trả về base price.
     */
    private BigDecimal getUnitPriceWithBulk(Product p, int quantity) {
        BigDecimal base = getBaseUnitPrice(p);
        if (p == null || p.getBulkDiscounts() == null || p.getBulkDiscounts().isEmpty()) {
            return base;
        }

        BigDecimal best = base;
        for (Product.BulkDiscount d : p.getBulkDiscounts()) {
            if (d == null) continue;
            Integer from = d.getFromQuantity();
            Integer to = d.getToQuantity();
            BigDecimal bulkUnit = d.getUnitPrice();

            if (bulkUnit == null) continue;
            int q = quantity;

            // from null => 1, to null => vô hạn
            int fromQ = (from == null ? 1 : from);
            int toQ = (to == null ? Integer.MAX_VALUE : to);

            if (q >= fromQ && q <= toQ) {
                // Nếu match nhiều khoảng, bạn có thể chọn khoảng có giá thấp nhất.
                if (best == null || bulkUnit.compareTo(best) < 0) {
                    best = bulkUnit;
                }
            }
        }
        return best;
    }

    private static String key(CartItemType type, UUID refId, UUID variantId) {
        // refId: productId nếu PRODUCT, comboId nếu COMBO
        String v = (variantId != null ? variantId.toString() : "_");
        String r = (refId != null ? refId.toString() : "_");
        return type.name() + ":" + r + ":" + v;
    }

    private boolean matchesCartItem(CartItem it, CheckoutItemRequest req) {
        CartItemType type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));

        if (type == CartItemType.COMBO) {
            UUID comboId = req.getComboId();
            return comboId != null
                    && it.getType() == CartItemType.COMBO
                    && it.getCombo() != null
                    && comboId.equals(it.getCombo().getComboId());
        } else {
            // PRODUCT
            UUID productId = req.getProductId();
            UUID variantId = req.getVariantId();

            UUID itemProductId = it.getProductIdOrNull();
            UUID itemVariantId = it.getVariantIdOrNull();

            // Nếu request có variantId => match theo variant
            if (variantId != null) {
                return it.getType() == CartItemType.PRODUCT
                        && itemVariantId != null
                        && variantId.equals(itemVariantId);
            }

            // Không có variantId => match productId và item không có variant
            if (productId != null) {
                return it.getType() == CartItemType.PRODUCT
                        && productId.equals(itemProductId)
                        && itemVariantId == null;
            }

            return false;
        }
    }

    private StoreAddressEntity resolveStoreOriginAddress(Store store) {
        if (store == null || store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty()) {
            return null;
        }

        // Ưu tiên địa chỉ defaultAddress = true
        return store.getStoreAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getDefaultAddress()))
                .findFirst()
                .orElse(store.getStoreAddresses().get(0)); // fallback: lấy địa chỉ đầu tiên
    }
    /**
     * Tính giá base theo variant/product, rồi áp campaign (nếu có).
     */
    private BigDecimal resolveUnitPrice(Product product,
                                        ProductVariantEntity variant,
                                        int quantity) {
        if (product == null) return BigDecimal.ZERO;

        // 1) Base price: nếu có variant → lấy variantPrice, không thì lấy theo bulk
        BigDecimal basePrice;
        if (variant != null) {
            basePrice = variant.getVariantPrice();
        } else {
            basePrice = getUnitPriceWithBulk(product, quantity);
        }
        if (basePrice == null) basePrice = BigDecimal.ZERO;

        // 2) Lấy list campaign active cho product này tại thời điểm hiện tại
        LocalDateTime now = LocalDateTime.now();
        List<PlatformCampaignProduct> cps =
                platformCampaignProductRepository.findAllActiveByProduct(product.getProductId(), now);

        if (cps == null || cps.isEmpty()) {
            // Không có chiến dịch active → trả giá base
            return basePrice;
        }

        // 3) Áp tất cả campaign, chọn giá thấp nhất (giảm nhiều nhất)
        BigDecimal bestPrice = basePrice;
        for (PlatformCampaignProduct cp : cps) {
            BigDecimal discounted = applyCampaignDiscount(basePrice, cp);
            if (discounted.compareTo(bestPrice) < 0) {
                bestPrice = discounted;
            }
        }

        return bestPrice;
    }

    /**
     * Áp giảm giá theo 1 record PlatformCampaignProduct
     * - Ưu tiên discountPercent, nếu không có thì dùng discountValue
     * - Có maxDiscountValue thì cap lại.
     */
    private BigDecimal applyCampaignDiscount(BigDecimal basePrice,
                                             PlatformCampaignProduct cp) {
        if (basePrice == null) return BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        // Giảm theo %
        if (cp.getDiscountPercent() != null && cp.getDiscountPercent() > 0) {
            discountAmount = basePrice
                    .multiply(BigDecimal.valueOf(cp.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        }

        // Nếu không có % mà có giá cố định
        if ((cp.getDiscountPercent() == null || cp.getDiscountPercent() == 0)
                && cp.getDiscountValue() != null
                && cp.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = cp.getDiscountValue();
        }

        // Giới hạn maxDiscountValue nếu có
        if (cp.getMaxDiscountValue() != null
                && discountAmount.compareTo(cp.getMaxDiscountValue()) > 0) {
            discountAmount = cp.getMaxDiscountValue();
        }

        BigDecimal result = basePrice.subtract(discountAmount);
        if (result.compareTo(BigDecimal.ZERO) < 0) result = BigDecimal.ZERO;

        // Optional: lưu lại original/discounted để report
        cp.setOriginalPrice(basePrice);
        cp.setDiscountedPrice(result);
        // Không bắt buộc save ở đây (tránh N+1), nên mình không gọi repo.save(cp).

        return result;
    }

    /**
     * Trừ tồn kho cho list CartItem khi checkout thành công.
     * - PRODUCT + variant: trừ cả variantStock và product.stockQuantity
     * - PRODUCT không variant: trừ product.stockQuantity
     * (COMBO hiện tại không đụng tới stockProducts, chỉ check stock combo ở chỗ khác)
     */
    private void deductStockForCartItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) return;

        // Dùng map để tránh trừ trùng 1 product/variant nhiều lần nếu có nhiều CartItem
        Map<UUID, Integer> productQtyMap = new HashMap<>();
        Map<UUID, Integer> variantQtyMap = new HashMap<>();

        for (CartItem item : items) {
            if (item.getType() != CartItemType.PRODUCT || item.getProduct() == null) {
                continue; // bỏ qua COMBO
            }

            int qty = item.getQuantity();
            if (qty <= 0) continue;

            Product p = item.getProduct();
            productQtyMap.merge(p.getProductId(), qty, Integer::sum);

            ProductVariantEntity v = item.getVariant();
            if (v != null) {
                variantQtyMap.merge(v.getId(), qty, Integer::sum);
            }
        }

        // 1) Trừ variant.stock
        for (CartItem item : items) {
            if (item.getType() != CartItemType.PRODUCT) continue;
            ProductVariantEntity v = item.getVariant();
            if (v == null) continue;

            int totalQty = variantQtyMap.getOrDefault(v.getId(), 0);
            if (totalQty <= 0) continue;

            Integer stock = v.getVariantStock();
            if (stock == null) stock = 0;

            if (stock < totalQty) {
                throw new IllegalStateException(
                        "Variant out of stock when checkout: "
                                + v.getOptionName() + " " + v.getOptionValue()
                );
            }
            v.setVariantStock(stock - totalQty);
            // Không cần gọi save riêng, JPA dirty checking sẽ tự flush vì đang trong @Transactional
        }

        // 2) Trừ product.stockQuantity
        for (CartItem item : items) {
            if (item.getType() != CartItemType.PRODUCT || item.getProduct() == null) continue;

            Product p = item.getProduct();
            int totalQty = productQtyMap.getOrDefault(p.getProductId(), 0);
            if (totalQty <= 0) continue;

            Integer stock = p.getStockQuantity();
            if (stock == null) stock = 0;

            if (stock < totalQty) {
                throw new IllegalStateException(
                        "Product out of stock when checkout: " + p.getName()
                );
            }
            p.setStockQuantity(stock - totalQty);
        }
    }

}