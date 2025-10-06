package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreKyc;
import org.example.audio_ecommerce.repository.StoreKycRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreKycService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreKycServiceImpl implements StoreKycService {

    private final StoreRepository storeRepository;
    private final StoreKycRepository storeKycRepository;

    @Override
    public StoreKyc submitKyc(UUID storeId, StoreKycRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        boolean hasPending = storeKycRepository.existsByStore_StoreIdAndStatus(storeId, KycStatus.PENDING);
        if (hasPending) throw new IllegalStateException("Store da gui don KYC va dang cho duyet");

        StoreKyc kyc = StoreKyc.builder()
                .id(UUID.randomUUID().toString())
                .store(store)
                .version(1)
                .storeName(request.getStoreName())
                .phoneNumber(request.getPhoneNumber())
                .businessLicenseNumber(request.getBusinessLicenseNumber())
                .taxCode(request.getTaxCode())
                .bankName(request.getBankName())
                .bankAccountName(request.getBankAccountName())
                .bankAccountNumber(request.getBankAccountNumber())
                .idCardFrontUrl(request.getIdCardFrontUrl())
                .idCardBackUrl(request.getIdCardBackUrl())
                .isOfficial(request.isOfficial())
                .businessLicenseUrl(request.getBusinessLicenseUrl())
                .status(KycStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        storeKycRepository.save(kyc);
        store.setStatus(StoreStatus.PENDING);
        storeRepository.save(store);
        return kyc;
    }

    @Override
    public void approveKyc(String kycId) {
        StoreKyc kyc = storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));
        kyc.setStatus(KycStatus.APPROVED);
        kyc.setReviewedAt(LocalDateTime.now());
        storeKycRepository.save(kyc);

        Store store = kyc.getStore();
        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);
    }

    @Override
    public void rejectKyc(String kycId, String reason) {
        StoreKyc kyc = storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));
        kyc.setStatus(KycStatus.REJECTED);
        kyc.setReviewNote(reason);
        kyc.setReviewedAt(LocalDateTime.now());
        storeKycRepository.save(kyc);

        Store store = kyc.getStore();
        store.setStatus(StoreStatus.REJECTED);
        storeRepository.save(store);
    }

    // 📜 Lấy danh sách request của 1 cửa hàng
    @Override
    public List<StoreKyc> getAllRequestsOfStore(UUID storeId) {
        return storeKycRepository.findByStore_StoreIdOrderByCreatedAtDesc(storeId);
    }

    // 📜 Lấy chi tiết request theo id
    @Override
    public StoreKyc getRequestDetail(String kycId) {
        return storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));
    }

    // 📜 Lấy tất cả request theo trạng thái (cho admin)
    @Override
    public List<StoreKyc> getRequestsByStatus(KycStatus status) {
        return storeKycRepository.findByStatusOrderBySubmittedAtDesc(status);
    }
}
