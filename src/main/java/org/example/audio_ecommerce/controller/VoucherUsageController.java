package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.VoucherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers/usage")
@RequiredArgsConstructor
public class VoucherUsageController {

    private final VoucherService voucherService;

    // ================= SHOP VOUCHER USAGE =================
    @GetMapping("/shop")
    public BaseResponse<Map<String, Object>> getShopUsage(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID voucherId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return voucherService.getShopVoucherUsage(storeId, voucherId, customerId, from, to, page, size);
    }

    // ================= PLATFORM VOUCHER USAGE =================
    @GetMapping("/platform")
    public BaseResponse<Map<String, Object>> getPlatformUsage(
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(required = false) UUID campaignProductId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return voucherService.getPlatformVoucherUsage(
                campaignId, campaignProductId, storeId, customerId, from, to, page, size
        );
    }
}
