package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.CartItemType;
import org.example.audio_ecommerce.entity.Enum.CartStatus;
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
