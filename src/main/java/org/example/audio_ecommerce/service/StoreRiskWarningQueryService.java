package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;

import java.util.UUID;

public interface StoreRiskWarningQueryService {
   // StoreRiskWarningResponse getMyRiskWarning();
    StoreRiskWarningResponse getRiskWarningByStoreId(UUID storeId);

}
