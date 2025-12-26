package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.StoreDebtBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/debts")
public class StoreDebtAdminController {

    private final StoreDebtBatchService storeDebtBatchService;

    @PostMapping("/recalc-all")
    public ResponseEntity<BaseResponse<?>> recalcAllStoreDebt() {

        int updated = storeDebtBatchService.recalcAllStoresDebt();

        return ResponseEntity.ok(
                BaseResponse.success("✅ Recalculated store debt balances", Map.of(
                        "updatedStores", updated
                ))
        );
    }
}
