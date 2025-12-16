package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreTopupCreateRequest;
import org.example.audio_ecommerce.dto.response.StoreTopupResponse;
import org.example.audio_ecommerce.service.PayOSEcomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores/{storeId}/wallet/topup")
public class StoreWalletTopupController {

    private final PayOSEcomService payOSEcomService;

    @Operation(summary = "Store tạo link PayOS để nạp tiền vào defaultBalance")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreTopupResponse createStoreTopup(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreTopupCreateRequest req
    ) {
        return payOSEcomService.createStoreWalletTopupPayment(
                storeId, req.getAmount(), req.getDescription(), req.getReturnUrl(), req.getCancelUrl()
        );
    }
}
