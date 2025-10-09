package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse addItems(UUID customerId, AddCartItemsRequest request);
    CartResponse getActiveCart(UUID customerId);
}
