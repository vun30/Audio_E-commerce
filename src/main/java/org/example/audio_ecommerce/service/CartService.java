package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AddToCartRequest;
import org.example.audio_ecommerce.dto.response.CartSummaryResponse;
import org.example.audio_ecommerce.entity.Cart;

import java.util.UUID;

public interface CartService {
    Cart addItems(UUID ownerId, AddToCartRequest req);
}
