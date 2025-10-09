//package org.example.audio_ecommerce.service.Impl;
//
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.request.AddToCartRequest;
//import org.example.audio_ecommerce.dto.response.CartItemResponse;
//import org.example.audio_ecommerce.dto.response.CartSummaryResponse;
//import org.example.audio_ecommerce.dto.response.StoreGroup;
//import org.example.audio_ecommerce.entity.*;
//import org.example.audio_ecommerce.entity.Enum.CartItemType;
//import org.example.audio_ecommerce.entity.Enum.CartStatus;
//import org.example.audio_ecommerce.repository.CartItemRepository;
//import org.example.audio_ecommerce.repository.CartRepository;
//import org.example.audio_ecommerce.repository.ProductComboRepository;
//import org.example.audio_ecommerce.repository.ProductRepository;
//import org.example.audio_ecommerce.service.CartService;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class CartServiceImpl implements CartService {
//
//    private final CartRepository cartRepo;
//    private final CartItemRepository cartItemRepo;
//    private final ProductRepository productRepo;
//    private final ProductComboRepository comboRepo;
//
//    // ===== Public APIs =====
//
//    @Override
//    @Transactional(readOnly = true)
//    public CartSummaryResponse getMyCart(UUID ownerId) {
//        Cart cart = getOrCreateActiveCart(ownerId);
//        return buildCartSummary(cart);
//    }
//
//    @Override
//    public CartSummaryResponse addToCart(UUID ownerId, AddToCartRequest req) {
//        if ((req.getProductId() == null && req.getComboId() == null)
//                || (req.getProductId() != null && req.getComboId() != null)) {
//            throw new IllegalArgumentException("Phải truyền productId HOẶC comboId");
//        }
//        int qty = Math.max(1, Optional.ofNullable(req.getQuantity()).orElse(1));
//        Cart cart = getOrCreateActiveCart(ownerId);
//
//        if (req.getProductId() != null) {
//            Product p = productRepo.findById(req.getProductId())
//                    .orElseThrow(() -> new NoSuchElementException("Product không tồn tại"));
//
//            BigDecimal unitPrice = (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
//                    ? p.getDiscountPrice()
//                    : Optional.ofNullable(p.getPrice()).orElse(BigDecimal.ZERO);
//
//            CartItem item = cartItemRepo.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), p.getProductId())
//                    .orElse(null);
//            if (item == null) {
//                item = CartItem.builder()
//                        .cart(cart)
//                        .itemType(CartItemType.PRODUCT)
//                        .product(p)
//                        .displayName(p.getName())
//                        .storeId(p.getStore() != null ? p.getStore().getStoreId() : null)
//                        .quantity(qty)
//                        .unitPrice(unitPrice)
//                        .selected(true)
//                        .build();
//            } else {
//                item.setQuantity(item.getQuantity() + qty);
//            }
//            item.recomputeSubtotal();
//            cartItemRepo.save(item);
//
//        } else {
//            ProductCombo combo = comboRepo.findById(req.getComboId())
//                    .orElseThrow(() -> new NoSuchElementException("Combo không tồn tại"));
//
//            BigDecimal unitPrice = Optional.ofNullable(combo.getComboPrice()).orElse(BigDecimal.ZERO);
//
//            CartItem item = cartItemRepo.findByCart_CartIdAndCombo_ComboId(cart.getCartId(), combo.getComboId())
//                    .orElse(null);
//            if (item == null) {
//                item = CartItem.builder()
//                        .cart(cart)
//                        .itemType(CartItemType.COMBO)
//                        .combo(combo)
//                        .displayName(combo.getComboProduct() != null ? combo.getComboProduct().getName() : "Combo")
//                        .storeId(combo.getStore() != null ? combo.getStore().getStoreId() : null)
//                        .quantity(qty)
//                        .unitPrice(unitPrice)
//                        .selected(true)
//                        .build();
//            } else {
//                item.setQuantity(item.getQuantity() + qty);
//            }
//            item.recomputeSubtotal();
//            cartItemRepo.save(item);
//        }
//        return buildCartSummary(cartRepo.findById(cart.getCartId()).orElseThrow());
//    }
//
//    @Override
//    public CartSummaryResponse toggleItem(UUID ownerId, UUID cartItemId, boolean selected) {
//        Cart cart = getOrCreateActiveCart(ownerId);
//        CartItem item = cartItemRepo.findById(cartItemId)
//                .orElseThrow(() -> new NoSuchElementException("Cart item không tồn tại"));
//        ensureItemBelongsToCart(item, cart);
//        item.setSelected(selected);
//        cartItemRepo.save(item);
//        return buildCartSummary(cartRepo.findById(cart.getCartId()).orElseThrow());
//    }
//
//    @Override
//    public CartSummaryResponse updateQuantity(UUID ownerId, UUID cartItemId, int quantity) {
//        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải > 0");
//        Cart cart = getOrCreateActiveCart(ownerId);
//        CartItem item = cartItemRepo.findById(cartItemId)
//                .orElseThrow(() -> new NoSuchElementException("Cart item không tồn tại"));
//        ensureItemBelongsToCart(item, cart);
//        item.setQuantity(quantity);
//        item.recomputeSubtotal();
//        cartItemRepo.save(item);
//        return buildCartSummary(cartRepo.findById(cart.getCartId()).orElseThrow());
//    }
//
//    @Override
//    public CartSummaryResponse removeItem(UUID ownerId, UUID cartItemId) {
//        Cart cart = getOrCreateActiveCart(ownerId);
//        CartItem item = cartItemRepo.findById(cartItemId)
//                .orElseThrow(() -> new NoSuchElementException("Cart item không tồn tại"));
//        ensureItemBelongsToCart(item, cart);
//        cartItemRepo.delete(item);
//        return buildCartSummary(cartRepo.findById(cart.getCartId()).orElseThrow());
//    }
//
//    // ===== Private helpers =====
//
//    private Cart getOrCreateActiveCart(UUID ownerId) {
//        return cartRepo.findByOwnerIdAndStatus(ownerId, CartStatus.ACTIVE)
//                .orElseGet(() -> cartRepo.save(
//                        Cart.builder().ownerId(ownerId).status(CartStatus.ACTIVE) .items(new ArrayList<>()).build()
//                ));
//    }
//
//    private void ensureItemBelongsToCart(CartItem item, Cart cart) {
//        if (!item.getCart().getCartId().equals(cart.getCartId())) {
//            throw new IllegalArgumentException("Item không thuộc giỏ của bạn");
//        }
//    }
//
//    private CartSummaryResponse buildCartSummary(Cart cart) {
//        Map<UUID, List<CartItem>> byStore = cart.getItems().stream()
//                .collect(Collectors.groupingBy(ci -> Optional.ofNullable(ci.getStoreId())
//                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
//
//        List<StoreGroup> groups = new ArrayList<>();
//        for (Map.Entry<UUID, List<CartItem>> e : byStore.entrySet()) {
//            UUID storeId = e.getKey();
//            List<CartItem> items = e.getValue();
//
//            StoreGroup g = new StoreGroup();
//            g.setStoreId(storeId.equals(zeroUuid()) ? null : storeId);
//            g.setStoreName(null); // map từ storeRepo nếu muốn
//            g.setItems(items.stream().map(this::toItemResp).toList());
//            groups.add(g);
//        }
//
//        CartSummaryResponse res = new CartSummaryResponse();
//        res.setCartId(cart.getCartId());
//        res.setStatus(cart.getStatus().name());
//        res.setGroups(groups);
//        int selectedCount = cart.getItems().stream()
//                .filter(CartItem::getSelected)
//                .mapToInt(CartItem::getQuantity)
//                .sum();
//        res.setSelectedCount(selectedCount);
//        res.setSelectedTotal(cart.getSelectedTotal());
//        return res;
//    }
//
//    private CartItemResponse toItemResp(CartItem it) {
//        CartItemResponse r = new CartItemResponse();
//        r.setCartItemId(it.getCartItemId());
//        r.setItemType(it.getItemType().name());
//        r.setProductId(it.getProduct() != null ? it.getProduct().getProductId() : null);
//        r.setComboId(it.getCombo() != null ? it.getCombo().getComboId() : null);
//        r.setStoreId(it.getStoreId());
//        r.setDisplayName(it.getDisplayName());
//        r.setQuantity(it.getQuantity());
//        r.setUnitPrice(it.getUnitPrice());
//        r.setSubtotal(it.getSubtotal());
//        r.setSelected(it.getSelected());
//
//        if (it.getProduct() != null && it.getProduct().getImages() != null) {
//            r.setImages(it.getProduct().getImages());
//        } else if (it.getCombo() != null && it.getCombo().getComboImageUrl() != null) {
//            r.setImages(List.of(it.getCombo().getComboImageUrl()));
//        } else {
//            r.setImages(List.of());
//        }
//        return r;
//    }
//
//    private static UUID zeroUuid() {
//        return UUID.fromString("00000000-0000-0000-0000-000000000000");
//    }
//}
