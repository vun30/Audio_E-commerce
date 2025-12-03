package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.email.dto.KycApprovedData;
import org.example.audio_ecommerce.email.dto.KycRejectedData;
import org.example.audio_ecommerce.email.dto.KycSubmittedData;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.*;
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
    private final EmailService emailService; // 👈 thêm vào

    // ==================== GỬI ĐƠN KYC ====================
    @Override
    public StoreKyc submitKyc(UUID storeId, StoreKycRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        boolean hasPending = storeKycRepository.existsByStore_StoreIdAndStatus(storeId, KycStatus.PENDING);
        if (hasPending)
            throw new IllegalStateException("Store đã gửi KYC và đang chờ duyệt");

//        // ✅ Kiểm tra business license number đã được đăng ký (APPROVED) bởi store khác
//        boolean licenseExists = storeKycRepository.existsByBusinessLicenseNumberAndStatus(
//                request.getBusinessLicenseNumber(),
//                KycStatus.APPROVED
//        );
//        if (licenseExists) {
//            throw new IllegalStateException("Số giấy phép kinh doanh này đã được đăng ký trong hệ thống");
//        }

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

        // 📧 Gửi mail xác nhận đã nhận đơn KYC
        try {
            KycSubmittedData mailData = KycSubmittedData.builder()
                    .email(store.getAccount().getEmail())
                    .ownerName(store.getAccount().getName())
                    .storeName(kyc.getStoreName())
                    .phoneNumber(kyc.getPhoneNumber())
                    .businessLicenseNumber(kyc.getBusinessLicenseNumber())
                    .businessLicenseUrl(kyc.getBusinessLicenseUrl())
                    .isOfficial(kyc.isOfficial())
                    .taxCode(kyc.getTaxCode())
                    .bankName(kyc.getBankName())
                    .bankAccountName(kyc.getBankAccountName())
                    .bankAccountNumber(kyc.getBankAccountNumber())
                    .idCardFrontUrl(kyc.getIdCardFrontUrl())
                    .idCardBackUrl(kyc.getIdCardBackUrl())
                    .submittedAt(kyc.getSubmittedAt())
                    .siteUrl("https://yourplatform.com/stores/me/kyc")
                    .build();

            emailService.sendEmail(EmailTemplateType.KYC_SUBMITTED, mailData);
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi gửi email KYC_SUBMITTED: " + e.getMessage());
        }

        return kyc;
    }

    // ==================== DUYỆT KYC ====================
    @Override
    @Transactional
    public void approveKyc(String kycId) {
        StoreKyc kyc = storeKycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        kyc.setStatus(KycStatus.APPROVED);
        kyc.setReviewedAt(LocalDateTime.now());
        storeKycRepository.save(kyc);

        Store store = kyc.getStore();
        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);

        // Nếu chưa có ví → tạo ví mặc định
        if (store.getWallet() == null) {
            StoreWallet wallet = StoreWallet.builder()
                    .store(store)
                    .availableBalance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .totalRevenue(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            store.setWallet(wallet);
            storeWalletRepository.save(wallet);
            storeRepository.save(store);

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

        // 📧 Gửi mail xác nhận được duyệt
        try {
            KycApprovedData mailData = KycApprovedData.builder()
                    .email(store.getAccount().getEmail())
                    .ownerName(store.getAccount().getName())
                    .storeName(store.getStoreName())
                    .siteUrl("https://yourplatform.com/stores/me")
                    .build();

            emailService.sendEmail(EmailTemplateType.KYC_APPROVED, mailData);
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi gửi email KYC_APPROVED: " + e.getMessage());
        }
    }

    // ==================== TỪ CHỐI KYC ====================
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

        // 📧 Gửi mail bị từ chối (có lý do)
        try {
            KycRejectedData mailData = KycRejectedData.builder()
                    .email(store.getAccount().getEmail())
                    .ownerName(store.getAccount().getName())
                    .storeName(store.getStoreName())
                    .reason(reason)
                    .siteUrl("https://yourplatform.com/stores/me/kyc")
                    .build();

            emailService.sendEmail(EmailTemplateType.KYC_REJECTED, mailData);
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi gửi email KYC_REJECTED: " + e.getMessage());
        }
    }

    // ==================== QUERY ====================
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

    // ==================== KIỂM TRA BUSINESS LICENSE ====================
    @Override
    public boolean checkBusinessLicenseExists(String businessLicenseNumber) {
        // Kiểm tra xem business license đã được APPROVED trong hệ thống chưa
        return storeKycRepository.existsByBusinessLicenseNumberAndStatus(
                businessLicenseNumber,
                KycStatus.APPROVED
        );
    }
}
