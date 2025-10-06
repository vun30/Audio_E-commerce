package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.KycStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_kyc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreKyc {

    @Id
    @Column(length = 36)
    private String id;
    // id don KYC (UUID duy nhat)

    @ManyToOne

    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;
    // cua hang lien ket voi don KYC nay

    @Column(nullable = false)
    private Integer version;
    // lan gui thu may cua cua hang (1,2,3...)

    @Column(name = "store_name", nullable = false)
    private String storeName;
    // ten cua hang hien thi tren nen tang

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    // so dien thoai cua hang

    @Column(name = "business_license_number", nullable = false)
    private String businessLicenseNumber;
    // so giay phep dang ky kinh doanh

    @Column(name = "tax_code")
    private String taxCode;
    // ma so thue cua cua hang / doanh nghiep

    @Column(name = "bank_name", nullable = false)
    private String bankName;
    // ten ngan hang cua tai khoan nhan tien

    @Column(name = "bank_account_name", nullable = false)
    private String bankAccountName;
    // ten chu tai khoan ngan hang

    @Column(name = "bank_account_number", nullable = false)
    private String bankAccountNumber;
    // so tai khoan ngan hang

    @Column(name = "id_card_front_url", nullable = false, length = 2000)
    private String idCardFrontUrl;
    // duong dan anh mat truoc CCCD/CMND

    @Column(name = "id_card_back_url", nullable = false, length = 2000)
    private String idCardBackUrl;
    // duong dan anh mat sau CCCD/CMND

    @Column(name = "is_official", nullable = false)
    private boolean isOfficial = false;
    // neu la cua hang chinh hang -> true

    @Column(name = "business_license_url", length = 2000)
    private String businessLicenseUrl;
    // neu la cua hang chinh hang thi co link giay phep kinh doanh upload len

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status = KycStatus.PENDING;
    // trang thai xet duyet KYC: PENDING, APPROVED, REJECTED

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String reviewNote;
    // ghi chu cua admin khi duyet (ly do tu choi, yeu cau bo sung...)

    private LocalDateTime submittedAt;
    // thoi diem cua hang gui don KYC

    private LocalDateTime reviewedAt;
    // thoi diem admin duyet don

    private LocalDateTime createdAt = LocalDateTime.now();
    // thoi diem tao ban ghi

    private LocalDateTime updatedAt = LocalDateTime.now();
    // thoi diem cap nhat gan nhat
}
