package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.StoreOrder;

public interface AutoCancelOrderService {
    void autoCancelWholeCustomerOrder(StoreOrder triggerStoreOrder, String reasonCode, String message);
}

