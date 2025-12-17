package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StorePayoutItemResponse;
import org.example.audio_ecommerce.dto.response.StorePayoutSummaryResponse;
import org.example.audio_ecommerce.entity.Enum.StorePayoutBucket;

import java.time.LocalDateTime;

public interface StorePayoutQueryService {

    StorePayoutSummaryResponse getSummary(LocalDateTime from, LocalDateTime to);


    PagedResult<StorePayoutItemResponse> getBreakdownItems(
            StorePayoutBucket bucket,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );
}