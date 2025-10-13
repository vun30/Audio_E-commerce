package org.example.audio_ecommerce.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmittedData {

    // 🔹 Email người nhận (chủ tài khoản)
    private String email;

    // 🔹 Tên chủ cửa hàng
    private String ownerName;

    // 🔹 Tên cửa hàng
    private String storeName;

    // 🔹 Số điện thoại cửa hàng
    private String phoneNumber;

    // 🔹 Thông tin giấy phép kinh doanh
    private String businessLicenseNumber;
    private String businessLicenseUrl;
    private boolean isOfficial;

    // 🔹 Mã số thuế
    private String taxCode;

    // 🔹 Thông tin ngân hàng
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;

    // 🔹 Ảnh định danh cá nhân
    private String idCardFrontUrl;
    private String idCardBackUrl;

    // 🔹 Thời điểm gửi
    private LocalDateTime submittedAt;

    // 🔹 Link truy cập trang KYC (tuỳ chỉnh domain)
    private String siteUrl;
}
