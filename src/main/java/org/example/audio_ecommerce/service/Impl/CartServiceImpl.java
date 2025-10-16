package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.request.CheckoutItemRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.CartItemType;
import org.example.audio_ecommerce.entity.Enum.CartStatus;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionStatus;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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

    @Override
    @Transactional
    public CartResponse addItems(UUID customerId, AddCartItemsRequest request) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepo.save(Cart.builder().customer(customer).status(CartStatus.ACTIVE).build()));

        // Dùng key (type + refId) để merge
        Map<String, CartItem> existingMap = new HashMap<>();
        for (CartItem it : cart.getItems()) {
            String key = key(it.getType(), it.getReferenceId());
            existingMap.put(key, it);
        }

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

    @Override
    @Transactional
    public void checkout(UUID customerId, List<CheckoutItemRequest> items) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        // Filter items to checkout
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : items) {
            CartItemType type;
            try {
                type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type for cart item: " + req.getType() + ". Chỉ chấp nhận PRODUCT hoặc COMBO.");
            }
            UUID refId = req.getId();
            cart.getItems().stream()
                .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                .findFirst()
                .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }
        // Lấy ví customer
        Wallet wallet = walletRepository.findByCustomer_Id(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Customer wallet not found"));
        // Lấy ví platform
        List<PlatformWallet> platformWallets = platformWalletRepository.findByOwnerType(org.example.audio_ecommerce.entity.Enum.WalletOwnerType.PLATFORM);
        if (platformWallets.isEmpty()) throw new IllegalStateException("Platform wallet not found");
        PlatformWallet platformWallet = platformWallets.get(0);
        BigDecimal totalAmount = itemsToCheckout.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Kiểm tra số dư ví customer
        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            throw new IllegalStateException("Số dư ví không đủ để thanh toán");
        }
        // Trừ tiền ví customer
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(totalAmount));
        wallet.setLastTransactionAt(java.time.LocalDateTime.now());
        walletRepository.save(wallet);
        // Ghi transaction ví customer
        WalletTransaction cusTxn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(totalAmount.negate())
                .transactionType(WalletTransactionType.PAYMENT)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Thanh toán đơn hàng (selected items)")
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .build();
        walletTransactionRepository.save(cusTxn);
        // Cộng tiền ví platform
        BigDecimal pfBalanceBefore = platformWallet.getTotalBalance();
        platformWallet.setTotalBalance(platformWallet.getTotalBalance().add(totalAmount));
        platformWalletRepository.save(platformWallet);
        // Ghi transaction ví platform
        Wallet platformWalletEntity = walletRepository.findById(platformWallet.getId())
            .orElseThrow(() -> new IllegalStateException("Platform wallet entity not found"));
        WalletTransaction pfTxn = WalletTransaction.builder()
                .wallet(platformWalletEntity)
                .amount(totalAmount)
                .transactionType(WalletTransactionType.DEPOSIT)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Nhận tiền từ đơn hàng của customer (selected items)")
                .balanceBefore(pfBalanceBefore)
                .balanceAfter(platformWallet.getTotalBalance())
                .build();
        walletTransactionRepository.save(pfTxn);
        // ...phần tạo Order giữ nguyên hoặc cập nhật nếu cần...
        // Xóa các item đã checkout khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);
    }

    @Override
    @Transactional
    public void checkout(UUID customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        // Prepare all items for full checkout
        List<CheckoutItemRequest> allItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            CheckoutItemRequest req = new CheckoutItemRequest();
            req.setId(item.getReferenceId());
            req.setType(item.getType().name());
            req.setQuantity(item.getQuantity());
            allItems.add(req);
        }
        checkout(customerId, allItems);
    }

    @Override
    @Transactional
    public void checkoutCOD(UUID customerId, List<CheckoutItemRequest> items) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        // Lọc các item cần checkout
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : items) {
            CartItemType type;
            try {
                type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type for cart item: " + req.getType() + ". Chỉ chấp nhận PRODUCT hoặc COMBO.");
            }
            UUID refId = req.getId();
            cart.getItems().stream()
                .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                .findFirst()
                .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }
        // Gom nhóm theo storeId
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = null;
            if (item.getType() == CartItemType.PRODUCT && item.getProduct() != null) {
                storeId = item.getProduct().getStore().getStoreId();
            } else if (item.getType() == CartItemType.COMBO && item.getCombo() != null) {
                storeId = item.getCombo().getStore().getStoreId();
            }
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }
        // Tạo CustomerOrder
        CustomerOrder customerOrder = CustomerOrder.builder()
                .customer(customer)
                .createdAt(java.time.LocalDateTime.now())
                .status("PENDING")
                .build();
        List<CustomerOrderItem> customerOrderItems = new ArrayList<>();
        for (CartItem item : itemsToCheckout) {
            CustomerOrderItem coi = CustomerOrderItem.builder()
                    .customerOrder(customerOrder)
                    .type(item.getType().name())
                    .refId(item.getReferenceId())
                    .name(item.getNameSnapshot())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .storeId((item.getType() == CartItemType.PRODUCT && item.getProduct() != null) ? item.getProduct().getStore().getStoreId() : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null))
                    .build();
            customerOrderItems.add(coi);
        }
        customerOrder.setItems(customerOrderItems);
        // Lưu CustomerOrder và item
        customerOrder = customerOrderRepository.save(customerOrder);
        // Tạo StoreOrder cho từng store
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeId = entry.getKey();
            Store store = storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found: " + storeId));
            StoreOrder storeOrder = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status("PENDING")
                    .customerOrder(customerOrder)
                    .build();
            List<StoreOrderItem> storeOrderItems = new ArrayList<>();
            for (CartItem item : entry.getValue()) {
                StoreOrderItem soi = StoreOrderItem.builder()
                        .storeOrder(storeOrder)
                        .type(item.getType().name())
                        .refId(item.getReferenceId())
                        .name(item.getNameSnapshot())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build();
                storeOrderItems.add(soi);
            }
            storeOrder.setItems(storeOrderItems);
            storeOrderRepository.save(storeOrder);
        }
        // Xóa các item đã checkout khỏi cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);
    }

    @Override
    @Transactional
    public CustomerOrderResponse checkoutCODWithResponse(UUID customerId, List<CheckoutItemRequest> items) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Cart cart = cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active cart found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        List<CartItem> itemsToCheckout = new ArrayList<>();
        for (CheckoutItemRequest req : items) {
            CartItemType type;
            try {
                type = CartItemType.valueOf(req.getType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type for cart item: " + req.getType() + ". Chỉ chấp nhận PRODUCT hoặc COMBO.");
            }
            UUID refId = req.getId();
            cart.getItems().stream()
                .filter(it -> it.getType() == type && it.getReferenceId().equals(refId))
                .findFirst()
                .ifPresent(itemsToCheckout::add);
        }
        if (itemsToCheckout.isEmpty()) {
            throw new IllegalStateException("No matching items in cart for checkout");
        }
        Map<UUID, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : itemsToCheckout) {
            UUID storeId = null;
            if (item.getType() == CartItemType.PRODUCT && item.getProduct() != null) {
                storeId = item.getProduct().getStore().getStoreId();
            } else if (item.getType() == CartItemType.COMBO && item.getCombo() != null) {
                storeId = item.getCombo().getStore().getStoreId();
            }
            if (storeId == null) throw new IllegalStateException("Không xác định được store cho item");
            itemsByStore.computeIfAbsent(storeId, k -> new ArrayList<>()).add(item);
        }
        CustomerOrder customerOrder = CustomerOrder.builder()
                .customer(customer)
                .createdAt(java.time.LocalDateTime.now())
                .status("PENDING")
                .build();
        List<CustomerOrderItem> customerOrderItems = new ArrayList<>();
        for (CartItem item : itemsToCheckout) {
            CustomerOrderItem coi = CustomerOrderItem.builder()
                    .customerOrder(customerOrder)
                    .type(item.getType().name())
                    .refId(item.getReferenceId())
                    .name(item.getNameSnapshot())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .storeId((item.getType() == CartItemType.PRODUCT && item.getProduct() != null) ? item.getProduct().getStore().getStoreId() : (item.getCombo() != null ? item.getCombo().getStore().getStoreId() : null))
                    .build();
            customerOrderItems.add(coi);
        }
        customerOrder.setItems(customerOrderItems);
        customerOrder = customerOrderRepository.save(customerOrder);
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByStore.entrySet()) {
            UUID storeId = entry.getKey();
            Store store = storeRepo.findById(storeId).orElseThrow(() -> new NoSuchElementException("Store not found: " + storeId));
            StoreOrder storeOrder = StoreOrder.builder()
                    .store(store)
                    .createdAt(java.time.LocalDateTime.now())
                    .status("PENDING")
                    .customerOrder(customerOrder)
                    .build();
            List<StoreOrderItem> storeOrderItems = new ArrayList<>();
            for (CartItem item : entry.getValue()) {
                StoreOrderItem soi = StoreOrderItem.builder()
                        .storeOrder(storeOrder)
                        .type(item.getType().name())
                        .refId(item.getReferenceId())
                        .name(item.getNameSnapshot())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build();
                storeOrderItems.add(soi);
            }
            storeOrder.setItems(storeOrderItems);
            storeOrderRepository.save(storeOrder);
        }
        cart.getItems().removeAll(itemsToCheckout);
        cartRepo.save(cart);
        cartItemRepo.deleteAll(itemsToCheckout);
        CustomerOrderResponse resp = new CustomerOrderResponse();
        resp.setId(customerOrder.getId());
        resp.setStatus(customerOrder.getStatus());
        return resp;
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
}
