package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CustomerOrderResponse;
import org.example.audio_ecommerce.entity.CustomerOrder;

import java.util.List;
import java.util.UUID;

public interface CartService {
    CartResponse addItems(UUID customerId, AddCartItemsRequest request);
    CartResponse getActiveCart(UUID customerId);
    CustomerOrderResponse checkoutCODWithResponse(UUID customerId, CheckoutCODRequest request);
    CustomerOrder createOrderForOnline(UUID customerId, CheckoutCODRequest request);
    CodEligibilityResponse checkCodEligibility(UUID customerId, List<CheckoutItemRequest> items);
    CartResponse updateItemQuantity(UUID customerId, UpdateCartItemQtyRequest request);
    CartResponse removeItems(UUID customerId, RemoveCartItemRequest request);
    CartResponse clearCart(UUID customerId);
    CartResponse bulkUpdateQuantities(UUID customerId, BulkUpdateCartQtyRequest request);
}
