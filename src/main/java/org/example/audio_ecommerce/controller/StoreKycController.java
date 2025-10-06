package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.StoreKyc;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.service.StoreKycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/kyc")
@RequiredArgsConstructor
public class StoreKycController {

    private final StoreKycService storeKycService;

    // 📨 Gửi request KYC
    @PostMapping
    public ResponseEntity<StoreKyc> submitKyc(@PathVariable UUID storeId,
                                              @RequestBody StoreKycRequest request) {
        return ResponseEntity.ok(storeKycService.submitKyc(storeId, request));
    }

    // 📜 Lấy danh sách tất cả request của cửa hàng
    @GetMapping
    public ResponseEntity<List<StoreKyc>> getAllRequests(@PathVariable UUID storeId) {
        return ResponseEntity.ok(storeKycService.getAllRequestsOfStore(storeId));
    }

    // 📜 Lấy chi tiết 1 request cụ thể
    @GetMapping("/{kycId}")
    public ResponseEntity<StoreKyc> getRequestDetail(@PathVariable String kycId) {
        return ResponseEntity.ok(storeKycService.getRequestDetail(kycId));
    }

    // 📜 Admin: Lấy tất cả request theo trạng thái (VD: PENDING)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<StoreKyc>> getRequestsByStatus(@PathVariable KycStatus status) {
        return ResponseEntity.ok(storeKycService.getRequestsByStatus(status));
    }

    // ✅ Admin duyệt đơn
    @PatchMapping("/{kycId}/approve")
    public ResponseEntity<String> approve(@PathVariable String kycId) {
        storeKycService.approveKyc(kycId);
        return ResponseEntity.ok("KYC approved and store activated ✅");
    }

    // ❌ Admin từ chối đơn
    @PatchMapping("/{kycId}/reject")
    public ResponseEntity<String> reject(@PathVariable String kycId,
                                         @RequestParam String reason) {
        storeKycService.rejectKyc(kycId, reason);
        return ResponseEntity.ok("KYC rejected ❌: " + reason);
    }

      // ✅ Lấy tất cả request KYC theo trạng thái (Admin dùng)
    @GetMapping("/status")
    public ResponseEntity<BaseResponse> getAllKycByStatus(@RequestParam KycStatus status) {
        List<StoreKyc> list = storeKycService.getRequestsByStatus(status);
        return ResponseEntity.ok(
                new BaseResponse<>(200, "Danh sách KYC theo trạng thái: " + status, list)
        );
    }
}
