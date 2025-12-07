package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.config.CodConfig;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.dto.response.PreviewCampaignPriceResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.*;

import static org.example.audio_ecommerce.service.Impl.GhnFeeRequestBuilder.buildForStoreShipment;

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
    private final CustomerOrderRepository customerOrderRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final StoreRepository storeRepo;
    private final VoucherService voucherService;
    private final GhnFeeService ghnFeeService;
    private final ProductVariantRepository productVariantRepo;
    private final OrderCodeGeneratorService orderCodeGeneratorService;
    private final PlatformCampaignProductRepository platformCampaignProductRepository;
    private final NotificationCreatorService notificationCreatorService;
    private final PlatformFeeRepository platformFeeRepository;
    private final PlatformCampaignProductUsageRepository platformCampaignProductUsageRepository;


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

                    if (!isProductSellable(product)) {
                        throw new IllegalStateException(
                                "Product is not available: " + product.getName()
                                        + " (status=" + product.getStatus() + ")"
                        );
                    }
                } else {
                    // không có variant => bắt buộc phải có productId
                    if (productId == null) {
                        throw new IllegalArgumentException("Either productId or variantId must be provided for PRODUCT");
                    }
                    product = productRepo.findById(productId)
                            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

                    if (!isProductSellable(product)) {
                        throw new IllegalStateException(
                                "Product is not available: " + product.getName()
                                        + " (status=" + product.getStatus() + ")"
                        );
                    }
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
                    BigDecimal unitPrice = resolveUnitPriceForCustomer(product, variant, totalQty, customer);
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
                    BigDecimal unitPrice = resolveUnitPriceForCustomer(product, variant, totalQty, customer);
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

    private CartResponse toResponse(Cart cart) {
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

            // ✅ Variant info
            UUID variantId = ci.getVariantIdOrNull();
            String variantOptionName = ci.getVariantOptionNameSnapshot();
            String variantOptionValue = ci.getVariantOptionValueSnapshot();
            String variantUrl = (ci.getVariant() != null) ? ci.getVariant().getVariantUrl() : null;

            // ✅ TÍNH GIÁ CHIẾN DỊCH + CHECK USAGE_PER_USER
            BigDecimal baseUnitPrice = null;
            BigDecimal platformCampaignPrice = null;
            boolean inPlatformCampaign = false;
            Boolean campaignUsageExceeded = null;
            Integer campaignRemaining = null;

            if (ci.getType() == CartItemType.PRODUCT && ci.getProduct() != null) {
                Product p = ci.getProduct();
                ProductVariantEntity v = ci.getVariant();

                // basePrice: variantPrice hoặc bulk theo product
                if (v != null) {
                    baseUnitPrice = v.getVariantPrice();
                    if (baseUnitPrice == null) baseUnitPrice = getBaseUnitPrice(p);
                } else {
                    baseUnitPrice = getUnitPriceWithBulk(p, ci.getQuantity());
                }
                if (baseUnitPrice == null) baseUnitPrice = BigDecimal.ZERO;

                LocalDateTime now = LocalDateTime.now();
                List<PlatformCampaignProduct> cps =
                        platformCampaignProductRepository.findAllActiveByProductLegacy(p.getProductId(), now);

                BigDecimal bestCampaignPrice = baseUnitPrice;
                boolean hasCampaign = false;
                PlatformCampaignProduct bestCampaign = null;

                if (cps != null && !cps.isEmpty()) {
                    for (PlatformCampaignProduct cp : cps) {
                        BigDecimal discounted = applyCampaignDiscount(baseUnitPrice, cp);
                        if (discounted.compareTo(bestCampaignPrice) < 0) {
                            bestCampaignPrice = discounted;
                            hasCampaign = true;
                            bestCampaign = cp;
                        }
                    }
                }

                // Mặc định: nếu còn lượt thì vẫn được xem là inCampaign
                inPlatformCampaign = hasCampaign;
                platformCampaignPrice = hasCampaign ? bestCampaignPrice : null;
                campaignUsageExceeded = false;

                // ==== CHECK USAGE_PER_USER + USEDCOUNT ====
                if (hasCampaign != false && bestCampaign != null
                        && bestCampaign.getUsagePerUser() != null
                        && bestCampaign.getUsagePerUser() > 0) {

                    Integer usagePerUser = bestCampaign.getUsagePerUser();

                    PlatformCampaignProductUsage usage =
                            platformCampaignProductUsageRepository
                                    .findByCampaignProductAndCustomer(bestCampaign, cart.getCustomer())
                                    .orElse(null);

                    int usedCount = (usage != null && usage.getUsedCount() != null)
                            ? usage.getUsedCount()
                            : 0;

                    int remaining = usagePerUser - usedCount;
                    if (remaining < 0) remaining = 0;
                    campaignRemaining = remaining;

                    // Nếu đã dùng hết lượt → không còn được hưởng campaign
                    if (remaining <= 0) {
                        inPlatformCampaign = false;
                        platformCampaignPrice = null;      // ẩn giá campaign, FE chỉ thấy base price
                        campaignUsageExceeded = true;      // flag cho FE biết đã hết quyền lợi
                    }
                }
            }


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
                    .baseUnitPrice(baseUnitPrice)
                    .platformCampaignPrice(platformCampaignPrice)
                    .inPlatformCampaign(inPlatformCampaign)
                    .campaignUsageExceeded(campaignUsageExceeded)
                    .campaignRemaining(campaignRemaining)
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

        // Trừ tồn kho theo items chuẩn bị checkout
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

        // Lấy platform fee percentage hiện tại để snapshot vào StoreOrder
        BigDecimal platformFeePercentage = platformFeeRepository
                .findFirstByIsActiveTrueOrderByEffectiveDateDesc()
                .map(PlatformFee::getPercentage)
                .orElse(BigDecimal.ZERO);

        // Kết quả orders để trả về
        List<CustomerOrder> createdOrders = new ArrayList<>();

        // 4) Loop từng shop → tạo 1 CustomerOrder riêng
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeIdKey = entry.getKey();
            Store store = storeRepo.findById(storeIdKey)
                    .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeIdKey));
            storeCache.put(storeIdKey, store);

            String orderCode = orderCodeGeneratorService.nextOrderCode();

            // Lấy địa chỉ origin của shop
            StoreAddressEntity originAddr = resolveStoreOriginAddress(store);
            String fromDistrictCode = originAddr != null ? originAddr.getDistrictCode() : null;
            String fromWardCode = originAddr != null ? originAddr.getWardCode() : null;

            // 4a) Tính phí GHN cho shop này
            Integer serviceTypeIdForStore = Optional.ofNullable(serviceTypeIds)
                    .map(m -> m.get(storeIdKey))
                    .orElse(5);

            var reqGHN = buildForStoreShipment(
                    entry.getValue(),
                    toDistrictId,
                    toWardCode,
                    fromDistrictCode,
                    fromWardCode,
                    serviceTypeIdForStore
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

            // 4c) Items của riêng shop này (dùng chung logic snapshot cho cả CustomerOrderItem & StoreOrderItem)
            List<CustomerOrderItem> coItems = new ArrayList<>();
            List<StoreOrderItem> soItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                // ==== TÍNH SNAPSHOT GIÁ GIỐNG BÊN STORE_ORDER_ITEM ====
                BigDecimal baseListUnit = ci.getType() == CartItemType.COMBO
                        ? Optional.ofNullable(ci.getUnitPrice()).orElse(BigDecimal.ZERO)
                        : (ci.getVariant() != null
                        ? Optional.ofNullable(ci.getVariant().getVariantPrice()).orElse(BigDecimal.ZERO)
                        : getBaseUnitPrice(ci.getProduct()));
                BigDecimal bulkUnit = ci.getType() == CartItemType.COMBO
                        ? baseListUnit
                        : (ci.getVariant() != null
                        ? baseListUnit
                        : getUnitPriceWithBulk(ci.getProduct(), ci.getQuantity()));
                BigDecimal lineBefore = baseListUnit
                        .multiply(BigDecimal.valueOf(ci.getQuantity()))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal platformPerUnit = bulkUnit.subtract(ci.getUnitPrice());
                if (platformPerUnit.compareTo(BigDecimal.ZERO) < 0) platformPerUnit = BigDecimal.ZERO;
                BigDecimal platformDiscount = platformPerUnit
                        .multiply(BigDecimal.valueOf(ci.getQuantity()))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal shopItemPerUnit = baseListUnit.subtract(bulkUnit);
                if (shopItemPerUnit.compareTo(BigDecimal.ZERO) < 0) shopItemPerUnit = BigDecimal.ZERO;
                BigDecimal shopItemDiscount = shopItemPerUnit
                        .multiply(BigDecimal.valueOf(ci.getQuantity()))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal totalItemDiscount = platformDiscount.add(shopItemDiscount);
                BigDecimal finalLine = lineBefore.subtract(totalItemDiscount);
                if (finalLine.compareTo(BigDecimal.ZERO) < 0) finalLine = BigDecimal.ZERO;
                BigDecimal finalUnit = finalLine.divide(
                        BigDecimal.valueOf(ci.getQuantity()), 0, RoundingMode.DOWN
                );
                // ==== CUSTOMER_ORDER_ITEM: đổ snapshot giống store ====
                CustomerOrderItem coi = CustomerOrderItem.builder()
                        .customerOrder(co)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        .unitPrice(ci.getUnitPrice()) // giá FE thấy ở cart (sau campaign/bulk)
                        .lineTotal(ci.getLineTotal()) // = unitPrice * qty (trước voucher)
                        .storeId(storeIdKey)
                        .unitPriceBeforeDiscount(baseListUnit.setScale(0, RoundingMode.DOWN))
                        .linePriceBeforeDiscount(lineBefore)
                        .platformVoucherDiscount(platformDiscount)
                        .shopItemDiscount(shopItemDiscount)
                        .shopOrderVoucherDiscount(BigDecimal.ZERO) // sẽ được cập nhật sau nếu allocate voucher per item
                        .totalItemDiscount(totalItemDiscount)
                        .finalUnitPrice(finalUnit)
                        .finalLineTotal(finalLine)
                        .amountCharged(finalLine) // số tiền thực sau mọi item-based discount
                        .build();
                coItems.add(coi);

                // ==== STORE_ORDER_ITEM: dùng lại y chang ====
                StoreOrderItem soi = StoreOrderItem.builder()
                        .storeOrder(null) // sẽ set sau khi có StoreOrder
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        // legacy fields
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        // snapshot fields
                        .unitPriceBeforeDiscount(baseListUnit.setScale(0, RoundingMode.DOWN))
                        .linePriceBeforeDiscount(lineBefore)
                        .platformVoucherDiscount(platformDiscount)
                        .shopItemDiscount(shopItemDiscount)
                        .shopOrderVoucherDiscount(BigDecimal.ZERO)
                        .totalItemDiscount(totalItemDiscount)
                        .finalUnitPrice(finalUnit)
                        .finalLineTotal(finalLine)
                        .amountCharged(finalLine)
                        .platformFeePercentage(platformFeePercentage)
                        .build();
                soItems.add(soi);
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
                    // snapshot platform fee percentage và actual shipping fee từ GHN
                    .platformFeePercentage(platformFeePercentage)
                    .actualShippingFee(shippingFee)
                    .build();
            so.setPaymentMethod(co.getPaymentMethod());

            // Gán StoreOrder vào các StoreOrderItem đã tạo ở trên
            for (StoreOrderItem soi : soItems) {
                soi.setStoreOrder(so);
            }
            so.setItems(soItems);
            storeOrderRepository.save(so);

            createNewOrderNotifications(co, store);
            // gom cho voucher service
            storeItemsMap.put(storeIdKey, soItems);
            createdOrders.add(co);
        }

        // === PHẦN SAU GIỮ NGUYÊN (voucher, cập nhật grand total, xóa cart...) ===
        // 5) Áp voucher theo shop + platform cho từng shop
        // 5) TÍNH voucher NỀN TẢNG TRƯỚC
        var platformResult = voucherService.computePlatformDiscounts(
                customerId,
                platformVouchers,
                storeItemsMap
        );

// 6) SAU ĐÓ mới tính voucher SHOP với base (subtotal - platformDiscount)
        var storeResult = voucherService.computeDiscountByStoreWithDetail(
                customerId,
                storeVouchers,
                storeItemsMap,
                platformResult.discountByStore   // map<storeId, platformDiscount>
        );
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

            co.setStoreDiscountTotal(storeDiscount);
            co.setPlatformDiscountTotal(platformDiscount);
            co.setDiscountTotal(discountTotal);
            co.setGrandTotal(grand);
            co.setPlatformVoucherDetailJson(platformResult.toPlatformVoucherJson());

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

                String storeJson = storeDetailJsonByStore.getOrDefault(sid, "{}");
                String platJson = platformDetailJsonByStore.getOrDefault(sid, "{}");
                so.setStoreVoucherDetailJson(storeJson);
                so.setPlatformVoucherDetailJson(platJson);

                // Allocate shop-order voucher to items proportionally
                if (sv != null && sv.compareTo(BigDecimal.ZERO) > 0) {
                    List<StoreOrderItem> items = Optional.ofNullable(so.getItems()).orElse(List.of());
                    BigDecimal subtotalBefore = items.stream()
                            .map(StoreOrderItem::getLinePriceBeforeDiscount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (subtotalBefore.compareTo(BigDecimal.ZERO) > 0 && !items.isEmpty()) {
                        BigDecimal allocated = BigDecimal.ZERO;
                        for (int i = 0; i < items.size(); i++) {
                            StoreOrderItem it = items.get(i);
                            BigDecimal share = it.getLinePriceBeforeDiscount()
                                    .multiply(sv)
                                    .divide(subtotalBefore, 0, RoundingMode.DOWN);
                            if (i == items.size() - 1) {
                                share = sv.subtract(allocated);
                            }
                            it.setShopOrderVoucherDiscount(share);

                            BigDecimal platformD = Optional.ofNullable(it.getPlatformVoucherDiscount()).orElse(BigDecimal.ZERO);
                            BigDecimal shopItemD = Optional.ofNullable(it.getShopItemDiscount()).orElse(BigDecimal.ZERO);
                            BigDecimal totalD = platformD.add(shopItemD).add(share);
                            it.setTotalItemDiscount(totalD);

                            BigDecimal lineBefore = Optional.ofNullable(it.getLinePriceBeforeDiscount()).orElse(BigDecimal.ZERO);
                            BigDecimal finalLine = lineBefore.subtract(totalD);
                            if (finalLine.compareTo(BigDecimal.ZERO) < 0) finalLine = BigDecimal.ZERO;
                            it.setFinalLineTotal(finalLine);
                            BigDecimal finalUnit = finalLine.divide(BigDecimal.valueOf(Math.max(it.getQuantity(), 1)), 0, RoundingMode.DOWN);
                            it.setFinalUnitPrice(finalUnit);
                            it.setAmountCharged(finalLine);

                            allocated = allocated.add(share);
                        }
                    }
                }

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

            BigDecimal unit = resolveUnitPriceForCustomer(p, v, q, customer);

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
                ProductVariantEntity v = item.getVariant();
                Integer stock = (v != null ? v.getVariantStock() : p.getStockQuantity());
                if (stock != null && stock < q) {
                    throw new IllegalStateException("Product out of stock: " + p.getName());
                }

                item.setQuantity(q);
                BigDecimal unit = resolveUnitPriceForCustomer(p, v, q, customer);
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
        BigDecimal campaignDiscountTotal = order.getItems().stream()
                .map(i -> {
                    BigDecimal lineBefore = Optional.ofNullable(i.getLinePriceBeforeDiscount()).orElse(BigDecimal.ZERO);
                    BigDecimal finalLine = Optional.ofNullable(i.getAmountCharged()).orElse(
                            Optional.ofNullable(i.getFinalLineTotal()).orElse(BigDecimal.ZERO)
                    );
                    return lineBefore.subtract(finalLine).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Lấy storeOrders
        var storeOrders = storeOrderRepository.findAllByCustomerOrder_Id(order.getId());
        UUID storeId = null;
        String storeName = null;
        Integer svTypeId = null;
        BigDecimal storeVoucherDiscountTotal = BigDecimal.ZERO;
        BigDecimal platformVoucherDiscount = BigDecimal.ZERO;

        StoreOrder firstSo = null;
        if (!storeOrders.isEmpty()) {
            firstSo = storeOrders.get(0);
            storeId = firstSo.getStore().getStoreId();
            storeName = firstSo.getStore().getStoreName();
            svTypeId = firstSo.getShippingServiceTypeId();
            storeVoucherDiscountTotal = Optional.ofNullable(firstSo.getStoreVoucherDiscount()).orElse(BigDecimal.ZERO);
            platformVoucherDiscount = Optional.ofNullable(firstSo.getPlatformVoucherDiscount()).orElse(BigDecimal.ZERO);
        }

        resp.setStoreId(storeId);
        resp.setStoreName(storeName);
        resp.setShippingServiceTypeId(svTypeId);
        resp.setCampaignDiscountTotal(campaignDiscountTotal);
        // Tổng số
        resp.setTotalAmount(Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO));
        resp.setShippingFeeTotal(Optional.ofNullable(order.getShippingFeeTotal()).orElse(BigDecimal.ZERO));

        // discountTotal ưu tiên lấy từ order (đã set sẵn), fallback nếu null
        BigDecimal discountTotal = Optional.ofNullable(order.getDiscountTotal())
                .orElse(storeVoucherDiscountTotal.add(platformVoucherDiscount));
        resp.setDiscountTotal(discountTotal);

        resp.setGrandTotal(Optional.ofNullable(order.getGrandTotal())
                .orElse(resp.getTotalAmount()
                        .add(resp.getShippingFeeTotal())
                        .subtract(discountTotal)));

        // ===== NEW: parse STORE voucher detail từ StoreOrder.storeVoucherDetailJson =====
        Map<String, BigDecimal> storeVoucherMap = new LinkedHashMap<>();
        try {
            if (firstSo != null && firstSo.getStoreVoucherDetailJson() != null) {
                var node = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(firstSo.getStoreVoucherDetailJson());
                node.fields().forEachRemaining(e -> {
                    storeVoucherMap.put(e.getKey(), new BigDecimal(e.getValue().asText("0")));
                });
            }
        } catch (Exception ignore) {
        }
        resp.setStoreVoucherDiscount(storeVoucherMap.isEmpty() ? null : storeVoucherMap);

        // ===== Platform voucher detail (giữ nguyên như cũ) =====
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

        // Lấy platform fee percentage hiện tại để snapshot vào StoreOrder
        BigDecimal platformFeePercentage = platformFeeRepository
                .findFirstByIsActiveTrueOrderByEffectiveDateDesc()
                .map(PlatformFee::getPercentage)
                .orElse(BigDecimal.ZERO);

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
                    // snapshot platform fee percentage (actual shipping = 0 cho store-ship)
                    .platformFeePercentage(platformFeePercentage)
                    .actualShippingFee(BigDecimal.ZERO)  // store-ship không có phí GHN
                    .build();
            so.setPaymentMethod(co.getPaymentMethod());

            List<StoreOrderItem> soItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                BigDecimal baseListUnit = ci.getType() == org.example.audio_ecommerce.entity.Enum.CartItemType.COMBO
                        ? Optional.ofNullable(ci.getUnitPrice()).orElse(BigDecimal.ZERO)
                        : (ci.getVariant() != null
                        ? Optional.ofNullable(ci.getVariant().getVariantPrice()).orElse(BigDecimal.ZERO)
                        : getBaseUnitPrice(ci.getProduct()));
                BigDecimal bulkUnit = ci.getType() == org.example.audio_ecommerce.entity.Enum.CartItemType.COMBO
                        ? baseListUnit
                        : (ci.getVariant() != null
                        ? baseListUnit
                        : getUnitPriceWithBulk(ci.getProduct(), ci.getQuantity()));
                BigDecimal lineBefore = baseListUnit.multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(0, RoundingMode.DOWN);

                BigDecimal platformPerUnit = bulkUnit.subtract(ci.getUnitPrice());
                if (platformPerUnit.compareTo(BigDecimal.ZERO) < 0) platformPerUnit = BigDecimal.ZERO;
                BigDecimal platformDiscount = platformPerUnit.multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(0, RoundingMode.DOWN);

                BigDecimal shopItemPerUnit = baseListUnit.subtract(bulkUnit);
                if (shopItemPerUnit.compareTo(BigDecimal.ZERO) < 0) shopItemPerUnit = BigDecimal.ZERO;
                BigDecimal shopItemDiscount = shopItemPerUnit.multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(0, RoundingMode.DOWN);

                BigDecimal totalItemDiscount = platformDiscount.add(shopItemDiscount);
                BigDecimal finalLine = lineBefore.subtract(totalItemDiscount);
                if (finalLine.compareTo(BigDecimal.ZERO) < 0) finalLine = BigDecimal.ZERO;
                BigDecimal finalUnit = finalLine.divide(BigDecimal.valueOf(ci.getQuantity()), 0, RoundingMode.DOWN);

                soItems.add(StoreOrderItem.builder()
                        .storeOrder(so)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
                        .variantId(ci.getVariantIdOrNull())
                        .variantOptionName(ci.getVariantOptionNameSnapshot())
                        .variantOptionValue(ci.getVariantOptionValueSnapshot())
                        // legacy fields
                        .unitPrice(ci.getUnitPrice())
                        .lineTotal(ci.getLineTotal())
                        // snapshot fields
                        .unitPriceBeforeDiscount(baseListUnit.setScale(0, RoundingMode.DOWN))
                        .linePriceBeforeDiscount(lineBefore)
                        .platformVoucherDiscount(platformDiscount)
                        .shopItemDiscount(shopItemDiscount)
                        .shopOrderVoucherDiscount(java.math.BigDecimal.ZERO)
                        .totalItemDiscount(totalItemDiscount)
                        .finalUnitPrice(finalUnit)
                        .finalLineTotal(finalLine)
                        .amountCharged(finalLine)
                        .platformFeePercentage(platformFeePercentage)
                        .build());
            }
            so.setItems(soItems);
            storeOrderRepository.save(so);

            storeItemsMap.put(storeIdKey, soItems);
            createdOrders.add(co);
        }

        // 5) Áp voucher như bình thường (không ảnh hưởng phí ship vì = 0)
        // 5) TÍNH voucher NỀN TẢNG TRƯỚC
        var platformResult = voucherService.computePlatformDiscounts(
                customerId,
                platformVouchers,
                storeItemsMap
        );

// 6) SAU ĐÓ mới tính voucher SHOP với base (subtotal - platformDiscount)
        var storeResult = voucherService.computeDiscountByStoreWithDetail(
                customerId,
                storeVouchers,
                storeItemsMap,
                platformResult.discountByStore   // map<storeId, platformDiscount>
        );

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

    /**
     * Giá base của product: ưu tiên discountPrice nếu > 0, fallback sang price.
     */
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
    // Public cũ: dùng khi không cần check usage_per_user (nếu chỗ nào khác đang gọi)
    private BigDecimal resolveUnitPrice(Product product,
                                        ProductVariantEntity variant,
                                        int quantity) {
        return resolveUnitPriceInternal(product, variant, quantity, null);
    }

    // NEW: dùng cho Cart, có customer để check usage_per_user
    private BigDecimal resolveUnitPriceForCustomer(Product product,
                                                   ProductVariantEntity variant,
                                                   int quantity,
                                                   Customer customer) {
        return resolveUnitPriceInternal(product, variant, quantity, customer);
    }

    // Internal: chứa toàn bộ logic base + campaign + usage_per_user
    private BigDecimal resolveUnitPriceInternal(Product product,
                                                ProductVariantEntity variant,
                                                int quantity,
                                                Customer customer) {
        if (product == null) return BigDecimal.ZERO;
        // DEBUG
        log.info("[CAMPAIGN-CHECK] productId={}, quantity={}, customerId={}",
                product.getProductId(), quantity, customer != null ? customer.getId() : null);
        // 1) Base price: variant -> variantPrice, nếu không thì theo bulk
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
        log.info("[CAMPAIGN-CHECK] campaignsFound={} for productId={} at now={}",
                (cps == null ? 0 : cps.size()), product.getProductId(), now);
        if (cps == null || cps.isEmpty()) {
            // Không có chiến dịch active → trả giá base
            return basePrice;
        }

        // 3) Áp tất cả campaign, chọn giá thấp nhất (giảm nhiều nhất)
        BigDecimal bestPrice = basePrice;
        PlatformCampaignProduct bestCampaign = null;

        for (PlatformCampaignProduct cp : cps) {
            BigDecimal discounted = applyCampaignDiscount(basePrice, cp);
            if (discounted.compareTo(bestPrice) < 0) {
                bestPrice = discounted;
                bestCampaign = cp;
            }
        }

        // 4) Nếu không có customer (chỗ dùng chung) → return như cũ
        if (customer == null || bestCampaign == null) {
            return bestPrice;
        }

        // 5) Check usage_per_user cho campaign áp dụng giá tốt nhất
        Integer usagePerUser = bestCampaign.getUsagePerUser();
        if (usagePerUser == null || usagePerUser <= 0) {
            // Không giới hạn số lần sử dụng → dùng bestPrice
            return bestPrice;
        }

        // Lấy usage hiện tại của customer cho campaignProduct này
        PlatformCampaignProductUsage usage =
                platformCampaignProductUsageRepository
                        .findByCampaignProductAndCustomer(bestCampaign, customer)
                        .orElse(null);

        int usedCount = (usage != null && usage.getUsedCount() != null)
                ? usage.getUsedCount()
                : 0;

        int remaining = usagePerUser - usedCount;

        // ❗ CASE 1: đã dùng hết hoặc vượt từ trước -> không còn ưu đãi → về base
        if (remaining <= 0) {
            return basePrice;
        }

        // ❗ CASE 2: số lượng mới (trong cart) > remaining
        // Ví dụ: usage_per_user = 1, usedCount = 0, quantity = 2
        // → vượt quyền lợi, theo yêu cầu: "quay về giá gốc cho 2 sản phẩm đó"
        if (quantity > remaining) {
            return basePrice;
        }

        // CASE 3: quantity <= remaining → vẫn được hưởng ưu đãi
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

        // FE sẽ tính và hiển thị giá discount dựa trên giá biến thể

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

        // ✅ Bước 0: kiểm tra trạng thái sản phẩm trước khi trừ stock
        for (CartItem item : items) {
            if (item.getType() != CartItemType.PRODUCT || item.getProduct() == null) {
                continue; // bỏ qua COMBO, hoặc item không có product
            }
            Product p = item.getProduct();

            // Sản phẩm đã bị ẩn / xoá / ngừng bán
            if (!isProductSellable(p)) {
                throw new IllegalStateException(
                        "Product is not available for checkout: " + p.getName()
                                + " (status=" + p.getStatus() + ")"
                );
            }
        }

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

    /**
     * Tạo notification khi có đơn hàng mới:
     * - Cho customer
     * - Cho store
     */
    private void createNewOrderNotifications(CustomerOrder co, Store store) {
        try {
            // CUSTOMER
            notificationCreatorService.createAndSend(
                    NotificationTarget.CUSTOMER,
                    co.getCustomer().getId(),
                    NotificationType.NEW_ORDER,
                    "Đặt hàng thành công",
                    "Đơn hàng " + co.getOrderCode()
                            + " tại cửa hàng " + store.getStoreName() + " đã được tạo thành công.",
                    "/customer/orders/" + co.getId(),
                    "{\"customerOrderId\":\"" + co.getId() + "\"}",
                    Map.of("screen", "ORDER_DETAIL")
            );

            // STORE
            String customerName = co.getCustomer() != null
                    ? co.getCustomer().getFullName()
                    : "Khách hàng";

            notificationCreatorService.createAndSend(
                    NotificationTarget.STORE,
                    store.getStoreId(),
                    NotificationType.NEW_ORDER,
                    "Bạn có đơn hàng mới",
                    "Bạn có đơn hàng mới " + co.getOrderCode()
                            + " từ " + customerName + ".",
                    "/seller/orders/" + co.getId(),
                    "{\"customerOrderId\":\"" + co.getId() + "\"}",
                    Map.of("screen", "SELLER_ORDER_DETAIL")
            );

        } catch (Exception e) {
            log.warn("Failed to create/send notifications for order {}", co.getId(), e);
        }
    }


    private boolean isProductSellable(Product p) {
        if (p == null) return false;
        return p.getStatus() == ProductStatus.ACTIVE;
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantityWithVouchers(UUID customerId,
                                                       UpdateCartItemQtyWithVoucherRequest request) {
        if (request.getCartItemId() == null
                || request.getQuantity() == null
                || request.getQuantity() < 1) {
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

        // ===== 1) Cập nhật quantity + unitPrice (đã có usage_per_user bên trong) =====
        if (item.getType() == CartItemType.PRODUCT && item.getProduct() != null) {
            Product p = item.getProduct();
            ProductVariantEntity v = item.getVariant();

            Integer stock = (v != null ? v.getVariantStock() : p.getStockQuantity());
            if (stock != null && stock < request.getQuantity()) {
                throw new IllegalStateException("Product/Variant out of stock: " + p.getName());
            }

            int q = request.getQuantity();
            item.setQuantity(q);

            BigDecimal unit = resolveUnitPriceForCustomer(p, v, q, customer);
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

        // ===== 2) TÍNH LẠI VOUCHER nếu cần (platform + store) =====
        // Giống phần trong checkout, nhưng đây chỉ là preview trên cart

        // Group items theo store để xây storeItemsMap cho voucherService
        Map<UUID, List<StoreOrderItem>> storeItemsMap = new HashMap<>();

        for (CartItem ci : Optional.ofNullable(cart.getItems()).orElse(List.of())) {
            UUID storeId = (ci.getType() == CartItemType.PRODUCT && ci.getProduct() != null)
                    ? ci.getProduct().getStore().getStoreId()
                    : (ci.getCombo() != null ? ci.getCombo().getStore().getStoreId() : null);
            if (storeId == null) continue;

            // build StoreOrderItem "ảo" để tính voucher
            StoreOrderItem soi = StoreOrderItem.builder()
                    .type(ci.getType().name())
                    .refId(ci.getReferenceId())
                    .name(ci.getNameSnapshot())
                    .quantity(ci.getQuantity())
                    .variantId(ci.getVariantIdOrNull())
                    .variantOptionName(ci.getVariantOptionNameSnapshot())
                    .variantOptionValue(ci.getVariantOptionValueSnapshot())
                    .unitPrice(ci.getUnitPrice())
                    .lineTotal(ci.getLineTotal())
                    .build();

            storeItemsMap.computeIfAbsent(storeId, k -> new ArrayList<>()).add(soi);
        }

        // Gọi voucher service (nếu FE có truyền voucher)
        var platformResult = voucherService.computePlatformDiscounts(
                customerId,
                request.getPlatformVouchers(),
                storeItemsMap
        );

        var storeResult = voucherService.computeDiscountByStoreWithDetail(
                customerId,
                request.getStoreVouchers(),
                storeItemsMap,
                platformResult.discountByStore
        );


        // ===== 3) Recalc subtotal / discount / grandTotal ở CART =====
        // (Tuỳ bạn muốn hiển thị tổng cart như nào, ở đây là đơn giản)

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // tổng discount từ voucher theo từng store
        BigDecimal totalStoreDiscount = storeResult.discountByStore.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformDiscount = platformResult.discountByStore.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = totalStoreDiscount.add(totalPlatformDiscount);
        BigDecimal grandTotal = subtotal.subtract(discountTotal);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

        cart.setSubtotal(subtotal);
        cart.setDiscountTotal(discountTotal);
        cart.setGrandTotal(grandTotal);

        cartRepo.save(cart);
        cartItemRepo.save(item);

        // ===== 4) Build CartResponse + cắm flag campaignUsageExceeded =====
        CartResponse resp = toResponse(cart);

        // tìm lại item tương ứng trong response
        for (CartResponse.Item itDto : resp.getItems()) {
            if (!itDto.getCartItemId().equals(item.getCartItemId())) continue;

            // tính basePrice & bestCampaignPrice không xét usage_per_user
            if (item.getType() == CartItemType.PRODUCT && item.getProduct() != null) {
                Product p = item.getProduct();
                ProductVariantEntity v = item.getVariant();

                BigDecimal basePrice;
                if (v != null) {
                    basePrice = Optional.ofNullable(v.getVariantPrice()).orElse(getBaseUnitPrice(p));
                } else {
                    basePrice = getUnitPriceWithBulk(p, item.getQuantity());
                }

                LocalDateTime now = LocalDateTime.now();
                List<PlatformCampaignProduct> cps =
                        platformCampaignProductRepository.findAllActiveByProductLegacy(p.getProductId(), now);

                BigDecimal bestCampaignPrice = basePrice;
                boolean hasCampaign = false;
                PlatformCampaignProduct bestCampaign = null;

                if (cps != null && !cps.isEmpty()) {
                    for (PlatformCampaignProduct cp : cps) {
                        BigDecimal discounted = applyCampaignDiscount(basePrice, cp);
                        if (discounted.compareTo(bestCampaignPrice) < 0) {
                            bestCampaignPrice = discounted;
                            hasCampaign = true;
                            bestCampaign = cp;
                        }
                    }
                }

                // ==== NEW: tính remaining usage ====
                Integer remaining = null;
                if (bestCampaign != null && bestCampaign.getUsagePerUser() != null) {
                    Integer usagePerUser = bestCampaign.getUsagePerUser();

                    PlatformCampaignProductUsage usage =
                            platformCampaignProductUsageRepository
                                    .findByCampaignProductAndCustomer(bestCampaign, customer)
                                    .orElse(null);

                    int usedCount = (usage != null && usage.getUsedCount() != null)
                            ? usage.getUsedCount()
                            : 0;

                    remaining = Math.max(usagePerUser - usedCount, 0);
                }

                boolean exceeded = false;
                if (hasCampaign
                        && itDto.getUnitPrice() != null
                        && itDto.getUnitPrice().compareTo(basePrice) == 0
                        && bestCampaignPrice.compareTo(basePrice) < 0
                        && item.getQuantity() > 1) {
                    exceeded = true;
                }

                itDto.setBaseUnitPrice(basePrice);
                itDto.setPlatformCampaignPrice(hasCampaign ? bestCampaignPrice : null);
                itDto.setInPlatformCampaign(hasCampaign);
                itDto.setCampaignUsageExceeded(exceeded);

                // === NEW: gán remaining vào response ===
                itDto.setCampaignRemaining(remaining);
            }

        }

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewCampaignPriceResponse previewCampaignPrice(
            UUID customerId,
            UUID productId,
            PreviewCampaignPriceRequest request
    ) {

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("quantity >= 1 is required");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));

        UUID variantId = request.getVariantId();
        int quantity = request.getQuantity();

        Product product;
        ProductVariantEntity variant = null;

        // Ưu tiên variantId nếu có
        if (variantId != null) {
            variant = productVariantRepo.findById(variantId)
                    .orElseThrow(() -> new NoSuchElementException("Variant not found: " + variantId));
            product = variant.getProduct();
            if (product == null) {
                throw new IllegalStateException("Variant has no product: " + variantId);
            }
            // optional: validate variant thuộc đúng product trên path
            if (productId != null && !product.getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to product");
            }
        } else {
            // không có variant => phải có productId trên path
            if (productId == null) {
                throw new IllegalArgumentException("productId is required");
            }
            product = productRepo.findById(productId)
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        }

        // check tồn kho
        Integer stock = (variant != null ? variant.getVariantStock() : product.getStockQuantity());
        if (stock != null && stock < quantity) {
            throw new IllegalStateException("Product/Variant out of stock: " + product.getName());
        }

        // ===== 1) Tính basePrice (không xét usage_per_user) =====
        BigDecimal basePrice;
        if (variant != null) {
            basePrice = Optional.ofNullable(variant.getVariantPrice())
                    .orElse(getBaseUnitPrice(product));
        } else {
            basePrice = getUnitPriceWithBulk(product, quantity);
        }
        if (basePrice == null) basePrice = BigDecimal.ZERO;

        // ===== 2) Tìm campaign tốt nhất (không xét usage_per_user) =====
        LocalDateTime now = LocalDateTime.now();
        List<PlatformCampaignProduct> cps =
                platformCampaignProductRepository.findAllActiveByProductLegacy(product.getProductId(), now);

        BigDecimal bestCampaignPrice = basePrice;
        boolean hasCampaign = false;
        PlatformCampaignProduct bestCampaign = null;

        if (cps != null && !cps.isEmpty()) {
            for (PlatformCampaignProduct cp : cps) {
                BigDecimal discounted = applyCampaignDiscount(basePrice, cp);
                if (discounted.compareTo(bestCampaignPrice) < 0) {
                    bestCampaignPrice = discounted;
                    hasCampaign = true;
                    bestCampaign = cp;
                }
            }
        }

        // ===== 3) Tính remaining usage cho campaign tốt nhất =====
        Integer remaining = null;
        if (hasCampaign && bestCampaign != null && bestCampaign.getUsagePerUser() != null) {
            Integer usagePerUser = bestCampaign.getUsagePerUser();

            PlatformCampaignProductUsage usage =
                    platformCampaignProductUsageRepository
                            .findByCampaignProductAndCustomer(bestCampaign, customer)
                            .orElse(null);

            int usedCount = (usage != null && usage.getUsedCount() != null)
                    ? usage.getUsedCount()
                    : 0;

            remaining = Math.max(usagePerUser - usedCount, 0);
        }

        // ===== 4) Tính effectiveUnitPrice (giống khi add/update cart) =====
        BigDecimal effectiveUnit = resolveUnitPriceForCustomer(product, variant, quantity, customer);
        if (effectiveUnit == null) effectiveUnit = BigDecimal.ZERO;

        BigDecimal lineTotal = effectiveUnit.multiply(BigDecimal.valueOf(quantity));

        // ===== 5) Flag usage_exceeded giống bên cart =====
        boolean exceeded = false;
        if (hasCampaign
                && effectiveUnit.compareTo(basePrice) == 0
                && bestCampaignPrice.compareTo(basePrice) < 0
                && quantity > 1) {
            exceeded = true;
        }

        return PreviewCampaignPriceResponse.builder()
                .productId(product.getProductId())
                .variantId(variant != null ? variant.getId() : null)
                .quantity(quantity)
                .baseUnitPrice(basePrice)
                .campaignUnitPrice(hasCampaign ? bestCampaignPrice : null)
                .effectiveUnitPrice(effectiveUnit)
                .lineTotal(lineTotal)
                .inCampaign(hasCampaign)
                .campaignUsageExceeded(exceeded)
                .campaignRemaining(remaining)
                .campaignName(bestCampaign != null && bestCampaign.getCampaign() != null
                        ? bestCampaign.getCampaign().getName()
                        : null)
                .campaignCode(bestCampaign != null && bestCampaign.getCampaign() != null
                        ? bestCampaign.getCampaign().getCode()
                        : null)
                .build();
    }


}