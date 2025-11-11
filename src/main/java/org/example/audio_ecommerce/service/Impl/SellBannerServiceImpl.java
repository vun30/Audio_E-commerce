package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;

import org.example.audio_ecommerce.dto.request.SellBannerCreateRequest;
import org.example.audio_ecommerce.dto.request.SellBannerUpdateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.SellBannerImageResponse;
import org.example.audio_ecommerce.dto.response.SellBannerResponse;
import org.example.audio_ecommerce.entity.SellBanner;
import org.example.audio_ecommerce.entity.SellBannerImage;
import org.example.audio_ecommerce.repository.SellBannerRepository;
import org.example.audio_ecommerce.service.SellBannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SellBannerServiceImpl implements SellBannerService {

    private final SellBannerRepository bannerRepo;

    // ==========================================
    // 🆕 Tạo banner
    // ==========================================
    @Override
    public ResponseEntity<BaseResponse> createBanner(SellBannerCreateRequest req) {
        SellBanner banner = toEntity(req);
        SellBanner saved = bannerRepo.save(banner);
        SellBannerResponse res = toResponse(saved);
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Tạo banner thành công", res));
    }

    // ==========================================
    // ✏️ Cập nhật banner
    // ==========================================
    @Override
    public ResponseEntity<BaseResponse> updateBanner(UUID id, SellBannerUpdateRequest req) {
        SellBanner existing = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Banner không tồn tại"));

        applyUpdate(existing, req);
        SellBanner saved = bannerRepo.save(existing);
        SellBannerResponse res = toResponse(saved);
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Cập nhật banner thành công", res));
    }

    // ==========================================
    // 📋 Lấy danh sách banner
    // ==========================================
    @Override
    public ResponseEntity<BaseResponse> getAllBanners(Boolean active) {
        List<SellBanner> list = (active != null && active)
                ? bannerRepo.findAllByActiveTrueOrderByCreatedAtDesc()
                : bannerRepo.findAll();

        List<SellBannerResponse> resList = list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Danh sách banner", resList));
    }

    // ==========================================
    // 🔍 Lấy banner theo ID
    // ==========================================
    @Override
    public ResponseEntity<BaseResponse> getBannerById(UUID id) {
        SellBanner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Banner không tồn tại"));

        SellBannerResponse res = toResponse(banner);
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Chi tiết banner", res));
    }

    // ==========================================
    // 🗑️ Xoá banner
    // ==========================================
    @Override
    public ResponseEntity<BaseResponse> deleteBanner(UUID id) {
        if (!bannerRepo.existsById(id))
            throw new RuntimeException("❌ Banner không tồn tại");

        bannerRepo.deleteById(id);
        return ResponseEntity.ok(new BaseResponse<>(200, "🗑️ Xóa banner thành công", null));
    }

    // ==========================================
    // 🧩 MAPPER NỘI BỘ
    // ==========================================

    // Convert CreateRequest -> Entity
    private SellBanner toEntity(SellBannerCreateRequest req) {
        if (req == null) return null;

        SellBanner banner = SellBanner.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .bannerType(req.getBannerType())
                .active(req.getActive() == null ? Boolean.TRUE : req.getActive())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();

        if (req.getImages() != null) {
            List<SellBannerImage> imgs = req.getImages().stream()
                    .map(i -> SellBannerImage.builder()
                            .banner(banner)
                            .imageUrl(i.getImageUrl())
                            .redirectUrl(i.getRedirectUrl())
                            .altText(i.getAltText())
                            .sortOrder(i.getSortOrder() == null ? 0 : i.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            banner.setImages(imgs);
        }
        return banner;
    }

    // Áp update từ UpdateRequest -> Entity
    private void applyUpdate(SellBanner existing, SellBannerUpdateRequest req) {
        if (req == null) return;

        if (req.getTitle() != null) existing.setTitle(req.getTitle());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());
        if (req.getBannerType() != null) existing.setBannerType(req.getBannerType());
        if (req.getActive() != null) existing.setActive(req.getActive());
        if (req.getStartTime() != null) existing.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) existing.setEndTime(req.getEndTime());

        // replace list ảnh
        if (req.getImages() != null) {
            existing.getImages().clear();
            List<SellBannerImage> imgs = req.getImages().stream()
                    .map(i -> SellBannerImage.builder()
                            .banner(existing)
                            .imageUrl(i.getImageUrl())
                            .redirectUrl(i.getRedirectUrl())
                            .altText(i.getAltText())
                            .sortOrder(i.getSortOrder() == null ? 0 : i.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            existing.getImages().addAll(imgs);
        }
    }

    // Entity -> Response
    private SellBannerResponse toResponse(SellBanner b) {
        if (b == null) return null;

        return SellBannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .description(b.getDescription())
                .bannerType(b.getBannerType())
                .active(b.getActive())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .images(b.getImages() == null ? List.of() :
                        b.getImages().stream().map(this::toImageResponse).collect(Collectors.toList()))
                .build();
    }

    private SellBannerImageResponse toImageResponse(SellBannerImage i) {
        if (i == null) return null;
        return SellBannerImageResponse.builder()
                .id(i.getId())
                .imageUrl(i.getImageUrl())
                .redirectUrl(i.getRedirectUrl())
                .altText(i.getAltText())
                .sortOrder(i.getSortOrder())
                .build();
    }
}
