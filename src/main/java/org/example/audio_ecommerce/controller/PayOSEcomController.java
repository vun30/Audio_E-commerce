package org.example.audio_ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CheckoutCODRequest;
import org.example.audio_ecommerce.dto.request.CheckoutOnlineRequest;
import org.example.audio_ecommerce.dto.request.WalletTopupRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.dto.response.WalletTopupResponse;
import org.example.audio_ecommerce.service.CartService;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.exception.APIException;
import vn.payos.exception.PayOSException;
import vn.payos.model.webhooks.WebhookData;   // <-- đúng package (không có .v2)

import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payos")
@RequiredArgsConstructor
public class PayOSEcomController {

    private final CartService cartService;
    private final PayOSEcomService payOSEcomService;
    private final PayOS payOS; // dùng SDK để verify webhook

    @PostMapping("/checkout")
    public ResponseEntity<BaseResponse<CheckoutOnlineResponse>> checkoutOnline(
            @RequestParam UUID customerId,
            @RequestBody CheckoutOnlineRequest req
    ) {
        var codReq = new CheckoutCODRequest();
        codReq.setAddressId(req.getAddressId());
        codReq.setMessage(req.getMessage());
        codReq.setItems(req.getItems());
        codReq.setStoreVouchers(req.getStoreVouchers());
        codReq.setPlatformVouchers(req.getPlatformVouchers());
        codReq.setServiceTypeIds(req.getServiceTypeIds());

        var orders = cartService.createOrderForOnline(customerId, codReq);

        java.math.BigDecimal total = orders.stream()
                .map(o -> o.getGrandTotal() == null ? java.math.BigDecimal.ZERO : o.getGrandTotal())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .setScale(0, java.math.RoundingMode.DOWN);

        long batchCode = System.currentTimeMillis() + new java.util.Random().nextInt(999);

        var pay = payOSEcomService.createPaymentForMultipleCustomerOrders(
                orders.stream().map(org.example.audio_ecommerce.dto.response.CustomerOrderResponse::getId).toList(),
                total,
                "Thanh toán " + orders.size() + " đơn",
                req.getReturnUrl(),
                req.getCancelUrl(),
                batchCode
        );

        return ResponseEntity.ok(BaseResponse.success("✅ Tạo link PayOS cho " + orders.size() + " đơn", pay));
    }

    /**
     * Webhook (SDK v2): đọc raw body, verify chữ ký bằng SDK → WebhookData.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(HttpServletRequest request) throws IOException {
        String body;
        try (BufferedReader br = request.getReader()) {
            body = br.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        try {
            // verify() nhận raw JSON (String/Object) và trả về WebhookData đã xác thực
            WebhookData webhookData = payOS.webhooks().verify(body);

            // Gọi service xử lý: service có thể nhận Object/WebhookData.
            // Nếu bạn đã sửa service theo gợi ý trước đó là confirmWebhook(Object),
            // truyền thẳng webhookData vào confirmWebhook(webhookData) là OK.
            payOSEcomService.confirmWebhook(webhookData);
            return ResponseEntity.ok().build();

        } catch (PayOSException e) {
            // chữ ký sai / payload không hợp lệ
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // các lỗi khác
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/wallet/checkout")
    public ResponseEntity<BaseResponse<WalletTopupResponse>> walletTopupCheckout(
            @RequestParam UUID customerId,
            @RequestBody WalletTopupRequest req
    ) {
        var resp = payOSEcomService.createWalletTopupPayment(
                customerId,
                req.getAmount(),
                "Nạp ví khách hàng",
                req.getReturnUrl(),
                req.getCancelUrl()
        );

        return ResponseEntity.ok(
                BaseResponse.success("✅ Tạo link PayOS để nạp ví", resp)
        );
    }
}
