package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class StoreKycRequest {
    private String storeName;
    private String phoneNumber;
    private String businessLicenseNumber;
    private String taxCode;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private boolean isOfficial;
    private String businessLicenseUrl;
}
