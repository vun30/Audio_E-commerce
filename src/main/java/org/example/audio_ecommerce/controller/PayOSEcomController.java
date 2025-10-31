package org.example.audio_ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CheckoutCODRequest;
import org.example.audio_ecommerce.dto.request.CheckoutOnlineRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CheckoutOnlineResponse;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.example.audio_ecommerce.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.Webhook;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payos")
@RequiredArgsConstructor
public class PayOSEcomController {

    private final CartService cartService;            // dùng lại logic tạo CustomerOrder & StoreOrders
    private final PayOSEcomService payOSEcomService;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tạo CustomerOrder + StoreOrders (như COD) rồi tạo PayOS link cho tổng số tiền.
     */
    @PostMapping("/checkout")
    public ResponseEntity<BaseResponse<CheckoutOnlineResponse>> checkoutOnline(@RequestParam UUID customerId,
                                                                 @RequestBody CheckoutOnlineRequest req) {
        // 1) Tạo CustomerOrder (y hệt COD) nhưng giữ status AWAITING_PAYMENT
        var codReq = new CheckoutCODRequest();
        codReq.setAddressId(req.getAddressId());
        codReq.setMessage(req.getMessage());
        codReq.setItems(req.getItems());
        codReq.setStoreVouchers(req.getStoreVouchers());

        var codResp = cartService.createOrderForOnline(customerId, codReq); // đã tạo orders + tính total

        // 2) Gọi PayOS tạo link
        CheckoutOnlineResponse pay = payOSEcomService.createPaymentForCustomerOrder(
                codResp.getId(),
                codResp.getGrandTotal(),
                "Thanh toán đơn hàng " + codResp.getId(),
                req.getReturnUrl(),
                req.getCancelUrl()
        );
        return ResponseEntity.ok(BaseResponse.success("✅ Tạo link PayOS thành công", pay));
    }

    /**
     * Webhook PayOS: xác nhận thanh toán và cập nhật CustomerOrder/StoreOrders.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(HttpServletRequest request) throws IOException {
        String body;
        try (BufferedReader br = request.getReader()) {
            body = br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        Webhook webhook = mapper.readValue(body, Webhook.class);
        payOSEcomService.confirmWebhook(webhook);
        return ResponseEntity.ok().build();
    }
}
