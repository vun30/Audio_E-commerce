package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.config.CodConfig;
import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.request.CheckoutCODRequest;
import org.example.audio_ecommerce.dto.request.CheckoutItemRequest;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.CartService;
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

                BigDecimal unit = c.getComboPrice() != null ? c.getComboPrice() : BigDecimal.ZERO;

                String k = key(type, c.getComboId());
                CartItem it = existingMap.get(k);
                if (it == null) {
                    it = CartItem.builder()
                            .cart(cart)
                            .type(type)
                            .combo(c)
                            .quantity(qty)
                            .unitPrice(unit)
                            .lineTotal(unit.multiply(BigDecimal.valueOf(qty)))
                            .nameSnapshot(c.getName())
                            .imageSnapshot(firstImage(c.getImages()))
                            .build();
                    cart.getItems().add(it);
                    existingMap.put(k, it);
                } else {
                    it.setQuantity(it.getQuantity() + qty);
                    it.setUnitPrice(unit);
                    it.setLineTotal(unit.multiply(BigDecimal.valueOf(it.getQuantity())));
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
                true // enforceCodDeposit = true (COD phải check)
        );

        BigDecimal totalAmount = customerOrder.getItems().stream()
                .map(CustomerOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerOrderResponse resp = new CustomerOrderResponse();
        resp.setId(customerOrder.getId());
        resp.setStatus(customerOrder.getStatus().name());
        resp.setMessage(customerOrder.getMessage());
        resp.setCreatedAt(customerOrder.getCreatedAt().toString());
        resp.setTotalAmount(totalAmount);

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
                false // enforceCodDeposit = false (online không check)
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
            boolean enforceCodDeposit
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

        // 2b) CHỈ COD mới check deposit
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

        // 4) Tạo CustomerOrder + snapshot địa chỉ
        CustomerOrder customerOrder = CustomerOrder.builder()
                .customer(customer)
                .createdAt(java.time.LocalDateTime.now())
                .message(message)
                .status(OrderStatus.PENDING) // hoặc AWAITING_PAYMENT nếu bạn có enum này
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

        // 7) Tạo StoreOrders
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeIdKey = entry.getKey();
            Store store = storeRepo.findById(storeIdKey)
                    .orElseThrow(() -> new NoSuchElementException("Store not found: " + storeIdKey));
            StoreOrder storeOrder = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .customerOrder(customerOrder)
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
                    .build();

            List<StoreOrderItem> storeOrderItems = entry.getValue().stream().map(item ->
                    StoreOrderItem.builder()
                            .storeOrder(storeOrder)
                            .type(item.getType().name())
                            .refId(item.getReferenceId())
                            .name(item.getNameSnapshot())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .build()
            ).collect(Collectors.toList());

            storeOrder.setItems(storeOrderItems);
            storeOrderRepository.save(storeOrder);
        }

        // 8) Xoá item khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);

        return customerOrder;
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
