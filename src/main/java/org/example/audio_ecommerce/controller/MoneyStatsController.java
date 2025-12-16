package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.MoneyStatsResponse;
import org.example.audio_ecommerce.service.MoneyStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class MoneyStatsController {

    private final MoneyStatsService moneyStatsService;

    @GetMapping("/money")
    public ResponseEntity<BaseResponse<MoneyStatsResponse>> getMoneyStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,

            // filter theo type cho từng hệ (optional)
            @RequestParam(required = false) String storeTxnType,
            @RequestParam(required = false) String customerTxnType,
            @RequestParam(required = false) String platformTxnType,
            @RequestParam(required = false) String platformStatus
    ) {
        MoneyStatsResponse data = moneyStatsService.getMoneyStats(
                from, to, storeId, customerId,
                storeTxnType, customerTxnType, platformTxnType, platformStatus
        );
        return ResponseEntity.ok(new BaseResponse<>(200, "OK", data));
    }
}
