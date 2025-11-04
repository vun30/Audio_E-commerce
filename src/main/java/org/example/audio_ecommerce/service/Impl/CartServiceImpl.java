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
    public List<CustomerOrderResponse> checkoutCODWithResponse(UUID customerId, CheckoutCODRequest request) {
        List<CustomerOrder> customerOrder = createOrdersSplitByStore(
                customerId,
                request.getItems(),
                request.getAddressId(),
                request.getMessage(),
                true,                        // enforceCodDeposit = false (COD bỏ qua)
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
                false, // online không check deposit
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
    protected List<CustomerOrder> createOrdersSplitByStore(
            UUID customerId,
            List<CheckoutItemRequest> itemsReq,
            UUID addressId,
            String message,
            boolean enforceCodDeposit,
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
            UUID refId = req.getId();
            cart.getItems().stream()
                    .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                    .findFirst()
                    .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }

        // 2) Group theo store
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = (item.getType() == CartItemType.PRODUCT && item.getProduct() != null)
                    ? item.getProduct().getStore().getStoreId()
                    : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null);
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }

        // 2b) (tuỳ chọn) enforce COD deposit theo shop
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

        Integer toDistrictId = addr.getDistrictId();
        String toWardCode = addr.getWardCode();

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

            // 4a) Tính phí GHN cho shop này
            Integer serviceTypeIdForStore = Optional.ofNullable(serviceTypeIds)
                    .map(m -> m.get(storeIdKey))
                    .orElse(5);
            var reqGHN = buildForStoreShipment(entry.getValue(), toDistrictId, toWardCode, serviceTypeIdForStore);
            BigDecimal shippingFee = extractTotalFee(ghnFeeService.calculateFeeRaw(reqGHN));

            // 4b) Tạo CustomerOrder cho shop
            CustomerOrder co = CustomerOrder.builder()
                    .customer(customer)
                    .createdAt(java.time.LocalDateTime.now())
                    .message(message)
                    .status(OrderStatus.PENDING)
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

            if (enforceCodDeposit) {
                co.setPaymentMethod(PaymentMethod.COD);
            } else {
                co.setPaymentMethod(PaymentMethod.ONLINE);
            }

            // 4c) Items của riêng shop này
            List<CustomerOrderItem> coItems = new ArrayList<>();
            for (CartItem ci : entry.getValue()) {
                coItems.add(CustomerOrderItem.builder()
                        .customerOrder(co)
                        .type(ci.getType().name())
                        .refId(ci.getReferenceId())
                        .name(ci.getNameSnapshot())
                        .quantity(ci.getQuantity())
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
        Map<UUID, BigDecimal> storeDiscountByStore =
                voucherService.computeDiscountByStore(storeVouchers, storeItemsMap);

        var platformResult = voucherService.computePlatformDiscounts(platformVouchers, storeItemsMap);

        // 6) Cập nhật từng CustomerOrder: discount/grand + JSON detail
        for (CustomerOrder co : createdOrders) {
            UUID storeIdOfOrder = co.getItems().stream()
                    .map(CustomerOrderItem::getStoreId)
                    .findFirst().orElse(null);

            BigDecimal storeDiscount = storeDiscountByStore.getOrDefault(storeIdOfOrder, BigDecimal.ZERO);
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

    private CustomerOrderResponse toOrderResponse(CustomerOrder order) {
        CustomerOrderResponse resp = new CustomerOrderResponse();
        resp.setId(order.getId());
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
        } catch (Exception ignore) {}
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

}