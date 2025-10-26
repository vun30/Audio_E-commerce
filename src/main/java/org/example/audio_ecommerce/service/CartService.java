package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AddCartItemsRequest;
import org.example.audio_ecommerce.dto.request.CheckoutItemRequest;
import org.example.audio_ecommerce.dto.request.CheckoutCODRequest;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;

import java.util.List;
import java.util.UUID;

public interface CartService {
    CartResponse addItems(UUID customerId, AddCartItemsRequest request);
    CartResponse getActiveCart(UUID customerId);
    CustomerOrderResponse checkoutCODWithResponse(UUID customerId, CheckoutCODRequest request);
    CodEligibilityResponse checkCodEligibility(UUID customerId, List<CheckoutItemRequest> items);
}
