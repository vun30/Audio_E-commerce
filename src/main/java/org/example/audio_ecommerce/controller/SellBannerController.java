package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.SellBannerCreateRequest;
import org.example.audio_ecommerce.dto.request.SellBannerUpdateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.SellBannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@Tag(name = "📢 Sell Banner API", description = "CRUD Banner quảng cáo (hiển thị trên trang chủ, chiến dịch, v.v.)")
public class SellBannerController {

    private final SellBannerService bannerService;

    // ======================
    // 🆕 CREATE
    // ======================
    @Operation(summary = "🆕 Tạo banner mới (có list ảnh + link)")
    @PostMapping
    public ResponseEntity<BaseResponse> createBanner(@RequestBody SellBannerCreateRequest req) {
        return bannerService.createBanner(req);
    }

    // ======================
    // ✏️ UPDATE
    // ======================
    @Operation(summary = "✏️ Cập nhật banner theo ID (thay list ảnh mới)")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> updateBanner(
            @PathVariable UUID id,
            @RequestBody SellBannerUpdateRequest req
    ) {
        return bannerService.updateBanner(id, req);
    }

    // ======================
    // 📋 GET ALL
    // ======================
    @Operation(summary = "📋 Lấy danh sách banner (filter active nếu cần)")
    @GetMapping
    public ResponseEntity<BaseResponse> getAllBanners(
            @RequestParam(required = false) Boolean active
    ) {
        return bannerService.getAllBanners(active);
    }

    // ======================
    // 🔍 GET BY ID
    // ======================
    @Operation(summary = "🔍 Lấy chi tiết banner theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getBannerById(@PathVariable UUID id) {
        return bannerService.getBannerById(id);
    }

    // ======================
    // 🗑️ DELETE
    // ======================
    @Operation(summary = "🗑️ Xóa banner theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> deleteBanner(@PathVariable UUID id) {
        return bannerService.deleteBanner(id);
    }
}
