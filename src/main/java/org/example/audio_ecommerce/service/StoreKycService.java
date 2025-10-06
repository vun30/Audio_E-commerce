package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.StoreKyc;

import java.util.List;
import java.util.UUID;

public interface StoreKycService {
    StoreKyc submitKyc(UUID storeId, StoreKycRequest request);
    void approveKyc(String kycId);
    void rejectKyc(String kycId, String reason);

    // 📜 View
    List <StoreKyc> getAllRequestsOfStore(UUID storeId);
    StoreKyc getRequestDetail(String kycId);
    List<StoreKyc> getRequestsByStatus(KycStatus status);
}