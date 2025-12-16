package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreRiskWarningController {

    private final StoreRiskWarningQueryService storeRiskWarningQueryService;

    @GetMapping("/me/risk-warning")
    public ResponseEntity<BaseResponse<StoreRiskWarningResponse>> getMyRiskWarning() {
        StoreRiskWarningResponse data = storeRiskWarningQueryService.getMyRiskWarning();
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy cảnh báo nợ thành công", data));
    }
}
