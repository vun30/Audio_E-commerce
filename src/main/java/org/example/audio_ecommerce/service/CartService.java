package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AddToCartRequest;
import org.example.audio_ecommerce.dto.response.CartSummaryResponse;

import java.util.UUID;

public interface CartService {

    /** Lấy giỏ ACTIVE của user (tự tạo nếu chưa có) và trả summary group theo store */
    CartSummaryResponse getMyCart(UUID ownerId);

    /** Thêm product hoặc combo vào giỏ */
    CartSummaryResponse addToCart(UUID ownerId, AddToCartRequest req);

    /** Tick chọn/bỏ chọn 1 item */
    CartSummaryResponse toggleItem(UUID ownerId, UUID cartItemId, boolean selected);

    /** Cập nhật số lượng 1 item */
    CartSummaryResponse updateQuantity(UUID ownerId, UUID cartItemId, int quantity);

    /** Xoá 1 item khỏi giỏ */
    CartSummaryResponse removeItem(UUID ownerId, UUID cartItemId);
}
