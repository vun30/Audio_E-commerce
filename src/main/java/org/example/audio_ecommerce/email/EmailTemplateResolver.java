package org.example.audio_ecommerce.email;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.email.dto.KycApprovedData;
import org.example.audio_ecommerce.email.dto.KycRejectedData;
import org.example.audio_ecommerce.email.dto.KycSubmittedData;
import org.example.audio_ecommerce.email.dto.StoreStatusChangedData;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class EmailTemplateResolver {

    private final TemplateEngine templateEngine;

    // =========================================
    // MAIN RESOLVER — chỉ còn 1 hàm duy nhất
    // =========================================
    public EmailTemplate resolve(EmailTemplateType type, Object data) {
        return switch (type) {
            case ACCOUNT_CREATED -> accountCreated((AccountData) data);
            case ACCOUNT_WELCOME -> accountWelcome((AccountData) data);
            case KYC_SUBMITTED -> kycSubmitted((KycSubmittedData) data);
            case KYC_APPROVED -> kycApproved((KycApprovedData) data);
            case KYC_REJECTED -> kycRejected((KycRejectedData) data);
            case ORDER_CONFIRMED -> orderConfirmed((OrderData) data);
            case RESET_PASSWORD -> resetPassword((AccountData) data);

            // ⭐⭐ CASE MỚI CHO SHOP STATUS ⭐⭐
            case STORE_STATUS_UPDATED -> storeStatusUpdated((StoreStatusChangedData) data);

            default -> throw new IllegalArgumentException("❌ Template chưa được định nghĩa: " + type);
        };
    }

    // ==================== ACCOUNT WELCOME ====================
    private EmailTemplate accountWelcome(AccountData data) {
        Context ctx = new Context();
        ctx.setVariable("name", data.getName());
        ctx.setVariable("role", data.getRole());
        ctx.setVariable("siteUrl", data.getSiteUrl());

        String template = switch (data.getRole()) {
            case "CUSTOMER" -> "email/welcome_customer";
            case "STOREOWNER" -> "email/welcome_store";
            case "ADMIN" -> "email/welcome_admin";
            default -> "email/welcome_customer";
        };

        String html = templateEngine.process(template, ctx);

        String subject = switch (data.getRole()) {
            case "CUSTOMER" -> "🎉 Chào mừng bạn đến với AudioEcommerce!";
            case "STOREOWNER" -> "🎉 Chào mừng bạn trở thành chủ cửa hàng!";
            case "ADMIN" -> "🔐 Tài khoản quản trị đã được khởi tạo";
            default -> "🎉 Chào mừng bạn!";
        };

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject(subject)
                .content(html)
                .build();
    }

    // ==================== ACCOUNT CREATED ====================
    private EmailTemplate accountCreated(AccountData data) {
        Context ctx = new Context();
        ctx.setVariable("name", data.getName());
        String html = templateEngine.process("email/account_created", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("🎉 Chào mừng bạn đến với AudioEcommerce!")
                .content(html)
                .build();
    }

    // ==================== KYC SUBMITTED ====================
    private EmailTemplate kycSubmitted(KycSubmittedData data) {
        Context ctx = new Context();
        ctx.setVariable("ownerName", data.getOwnerName());
        ctx.setVariable("storeName", data.getStoreName());
        ctx.setVariable("phoneNumber", data.getPhoneNumber());
        ctx.setVariable("businessLicenseNumber", data.getBusinessLicenseNumber());
        ctx.setVariable("taxCode", data.getTaxCode());
        ctx.setVariable("bankName", data.getBankName());
        ctx.setVariable("bankAccountName", data.getBankAccountName());
        ctx.setVariable("bankAccountNumber", data.getBankAccountNumber());
        ctx.setVariable("submittedAt", data.getSubmittedAt());
        ctx.setVariable("siteUrl", data.getSiteUrl());

        String html = templateEngine.process("email/kyc_submitted", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("📨 Đã nhận hồ sơ KYC cho cửa hàng " + data.getStoreName())
                .content(html)
                .build();
    }

    // ==================== KYC APPROVED ====================
    private EmailTemplate kycApproved(KycApprovedData data) {
        Context ctx = new Context();
        ctx.setVariable("ownerName", data.getOwnerName());
        ctx.setVariable("storeName", data.getStoreName());
        ctx.setVariable("siteUrl", data.getSiteUrl());

        String html = templateEngine.process("email/kyc_approved", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("✅ Cửa hàng " + data.getStoreName() + " đã được xác thực thành công!")
                .content(html)
                .build();
    }

    // ==================== KYC REJECTED ====================
    private EmailTemplate kycRejected(KycRejectedData data) {
        Context ctx = new Context();
        ctx.setVariable("ownerName", data.getOwnerName());
        ctx.setVariable("storeName", data.getStoreName());
        ctx.setVariable("reason", data.getReason());
        ctx.setVariable("siteUrl", data.getSiteUrl());

        String html = templateEngine.process("email/kyc_rejected", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("❌ Hồ sơ KYC của cửa hàng " + data.getStoreName() + " chưa được phê duyệt")
                .content(html)
                .build();
    }

    // ==================== ORDER CONFIRMED ====================
    private EmailTemplate orderConfirmed(OrderData data) {
        Context ctx = new Context();
        ctx.setVariable("customerName", data.getCustomerName());
        ctx.setVariable("orderCode", data.getOrderCode());
        ctx.setVariable("total", data.getTotalAmount());
        ctx.setVariable("paidAt", data.getPaidAt());
        ctx.setVariable("receiverName", data.getReceiverName());
        ctx.setVariable("shippingAddress", data.getShippingAddress());
        ctx.setVariable("phoneNumber", data.getPhoneNumber());
        ctx.setVariable("shippingNote", data.getShippingNote());
        ctx.setVariable("items", data.getItems());

        String html = templateEngine.process("email/order_confirmed", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("🛒 Đơn hàng #" + (data.getOrderCode() != null ? data.getOrderCode() : "") + " của bạn đã được xác nhận")
                .content(html)
                .build();
    }

    // ==================== RESET PASSWORD ====================
    private EmailTemplate resetPassword(AccountData data) {
        Context ctx = new Context();
        ctx.setVariable("name", data.getName());
        ctx.setVariable("resetLink", data.getSiteUrl());

        String html = templateEngine.process("email/reset_password", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("🔑 Đặt lại mật khẩu tài khoản của bạn")
                .content(html)
                .build();
    }

    // ==================== STORE STATUS UPDATED (NEW) ====================
    private EmailTemplate storeStatusUpdated(StoreStatusChangedData data) {
        Context ctx = new Context();

        ctx.setVariable("ownerName", data.getOwnerName());
        ctx.setVariable("storeName", data.getStoreName());
        ctx.setVariable("newStatus", data.getNewStatus());
        ctx.setVariable("reason", data.getReason());
        ctx.setVariable("siteUrl", data.getSiteUrl());

        String html = templateEngine.process("email/store_status_updated", ctx);

        return EmailTemplate.builder()
                .to(data.getEmail())
                .subject("📢 Cập nhật trạng thái cửa hàng: " + data.getStoreName())
                .content(html)
                .build();
    }
}
