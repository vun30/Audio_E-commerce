package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.entity.StoreOrderItem;

import java.math.BigDecimal;
import java.util.*;

public interface VoucherService {
    Map<UUID, BigDecimal> computeDiscountByStore(
            List<StoreVoucherUse> vouchersInput,
            Map<UUID, List<StoreOrderItem>> storeItems
    );
}
