package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreRiskWarningController {

    private final StoreRiskWarningQueryService storeRiskWarningQueryService;
    private final StoreRepository storeRepository;

    @GetMapping("/me/risk-warning")
    public ResponseEntity<BaseResponse<StoreRiskWarningResponse>> getMyRiskWarning() {

        String principal = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        String email = principal.contains(":")
                ? principal.split(":")[0]
                : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() ->
                        new RuntimeException("❌ Không tìm thấy store cho tài khoản"));

        StoreRiskWarningResponse data =
                storeRiskWarningQueryService.getRiskWarningByStoreId(store.getStoreId());

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy cảnh báo nợ thành công", data)
        );
    }
}
