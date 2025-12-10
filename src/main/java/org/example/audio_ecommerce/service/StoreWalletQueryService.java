// package org.example.audio_ecommerce.service;
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.StoreWalletItemResponse;
import org.example.audio_ecommerce.dto.response.StoreWalletSummaryFinalResponse;
import org.example.audio_ecommerce.entity.Enum.StoreWalletBucket;
import org.example.audio_ecommerce.dto.response.PagedResult; // bạn đã có class này trong project

import java.util.UUID;

public interface StoreWalletQueryService {

    StoreWalletSummaryFinalResponse getSummary(UUID storeId);

    PagedResult<StoreWalletItemResponse> getItemsByBucket(
            UUID storeId,
            StoreWalletBucket bucket,
            int page,
            int size
    );
}
