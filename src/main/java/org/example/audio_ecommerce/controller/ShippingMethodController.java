package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ShippingMethodRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ShippingMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Shipping Method", description = "API CRUD phương thức vận chuyển")
@RestController
@RequestMapping("/api/shipping-methods")
@RequiredArgsConstructor
public class ShippingMethodController {

    private final ShippingMethodService service;

    @Operation(summary = "Tạo phương thức vận chuyển mới")
    @PostMapping
    public ResponseEntity<BaseResponse> create(@RequestBody ShippingMethodRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Cập nhật phương thức vận chuyển")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(@PathVariable UUID id, @RequestBody ShippingMethodRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Xóa phương thức vận chuyển")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(@PathVariable UUID id) {
        return service.delete(id);
    }

    @Operation(summary = "Lấy danh sách tất cả phương thức vận chuyển")
    @GetMapping
    public ResponseEntity<BaseResponse> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Lấy thông tin chi tiết 1 phương thức vận chuyển")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable UUID id) {
        return service.getById(id);
    }
}
