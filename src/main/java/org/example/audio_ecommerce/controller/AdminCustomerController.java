package org.example.audio_ecommerce.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;

import org.example.audio_ecommerce.service.Impl.AdminCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @PatchMapping("/{customerId}/buyable")
    public ResponseEntity<?> updateBuyable(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerBuyableRequest req
    ) {
        adminCustomerService.setBuyable(customerId, req.getBuyable());
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "buyable", req.getBuyable()
        ));
    }
}

