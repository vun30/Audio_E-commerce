package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.PlatformFee;
import org.example.audio_ecommerce.service.PlatformFeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/platform-fees")
@RequiredArgsConstructor
public class PlatformFeeController {

    private final PlatformFeeService service;

    @PostMapping
    public ResponseEntity<PlatformFee> create(@RequestBody PlatformFee fee) {
        return ResponseEntity.ok(service.create(fee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformFee> update(@PathVariable UUID id, @RequestBody PlatformFee fee) {
        return ResponseEntity.ok(service.update(id, fee));
    }

    @GetMapping("/active")
    public ResponseEntity<PlatformFee> getActive() {
        return ResponseEntity.ok(service.getActiveFee());
    }

    @GetMapping
    public ResponseEntity<List<PlatformFee>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
