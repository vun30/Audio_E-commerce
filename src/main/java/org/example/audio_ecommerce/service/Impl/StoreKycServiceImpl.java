package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreKyc;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.StoreKycService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreKycServiceImpl implements StoreKycService {

    private final StoreRepository storeRepository;
    private final StoreKycRepository storeKycRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;

    @Override
    public StoreKyc submitKyc(UUID storeId, StoreKycRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        boolean hasPending = storeKycRepository.existsByStore_StoreIdAndStatus(storeId, KycStatus.PENDING);
        if (hasPending) throw new IllegalStateException("Store đã gửi KYC và đang chờ duyệt");

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

        // ✅ Cập nhật trạng thái store
        store.setStatus(StoreStatus.PENDING);
        storeRepository.save(store);

        return kyc;
    }

    @Override
    @Transactional
    public void approveKyc(String kycId) {
        StoreKyc kyc = storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        // ✅ 1️⃣ Cập nhật trạng thái KYC
        kyc.setStatus(KycStatus.APPROVED);
        kyc.setReviewedAt(LocalDateTime.now());
        storeKycRepository.save(kyc);

        // ✅ 2️⃣ Kích hoạt Store
        Store store = kyc.getStore();
        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);

        // ✅ 3️⃣ Nếu chưa có ví → tạo ví + transaction mặc định
        if (store.getWallet() == null) {
            StoreWallet wallet = StoreWallet.builder()
                    .store(store)
                    .availableBalance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .totalRevenue(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 🔗 Gắn ví ngược lại vào store
            store.setWallet(wallet);

            // 📥 Lưu cả ví và store
            storeWalletRepository.save(wallet);
            storeRepository.save(store);

            // ✅ 4️⃣ Tạo transaction khởi tạo ví
            StoreWalletTransaction initTran = StoreWalletTransaction.builder()
                    .wallet(wallet)
                    .type(StoreWalletTransactionType.ADJUSTMENT)
                    .amount(BigDecimal.ZERO)
                    .balanceAfter(BigDecimal.ZERO)
                    .description("📦 Ví cửa hàng được khởi tạo tự động khi KYC được duyệt")
                    .createdAt(LocalDateTime.now())
                    .build();

            storeWalletTransactionRepository.save(initTran);
        }
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

    @Override
    public List<StoreKyc> getAllRequestsOfStore(UUID storeId) {
        return storeKycRepository.findByStore_StoreIdOrderByCreatedAtDesc(storeId);
    }

    @Override
    public StoreKyc getRequestDetail(String kycId) {
        return storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));
    }

    @Override
    public List<StoreKyc> getRequestsByStatus(KycStatus status) {
        return storeKycRepository.findByStatusOrderBySubmittedAtDesc(status);
    }
}
