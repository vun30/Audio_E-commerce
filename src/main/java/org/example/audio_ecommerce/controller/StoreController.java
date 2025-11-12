package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest.StoreAddressRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreStatusRequest;
import org.example.audio_ecommerce.dto.request.StaffCreateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StaffResponse;
import org.example.audio_ecommerce.service.StoreService;
import org.example.audio_ecommerce.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Store", description = "Các API quản lý cửa hàng (Admin & Chủ shop)")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StaffService staffService;

    // =========================================================
    // 🏪 STORE CRUD (giữ nguyên)
    // =========================================================

    @Operation(summary = "Xem chi tiết cửa hàng")
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse> getStoreById(@PathVariable UUID storeId) {
        return storeService.getStoreById(storeId);
    }

    @Operation(summary = "Lấy cửa hàng theo tài khoản")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<BaseResponse> getStoreByAccount(@PathVariable UUID accountId) {
        return storeService.getStoreByAccountId(accountId);
    }

    @Operation(summary = "Cập nhật thông tin cửa hàng")
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request) {
        return storeService.updateStore(storeId, request);
    }

    @Operation(summary = "Cập nhật trạng thái cửa hàng")
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse> updateStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request) {
        return storeService.updateStoreStatus(storeId, request.getStatus());
    }

    @Operation(summary = "Danh sách cửa hàng (phân trang + tìm kiếm)")
    @GetMapping
    public ResponseEntity<BaseResponse> getAllStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return storeService.getAllStores(page, size, keyword);
    }

    @Operation(summary = "Lấy cửa hàng đang đăng nhập")
    @GetMapping("/me/id")
    public ResponseEntity<BaseResponse> getMyStoreId() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;
        var storeOpt = storeService.getStoreByEmail(email);

        if (storeOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new BaseResponse<>(404, "❌ Không tìm thấy cửa hàng cho tài khoản: " + email, null));
        }

        UUID storeId = storeOpt.get().getStoreId();
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy storeId thành công", storeId));
    }

    @Operation(summary = "Tạo staff cho cửa hàng")
    @PostMapping("/{storeId}/staff")
    public StaffResponse createStaff(@PathVariable UUID storeId, @Valid @RequestBody StaffCreateRequest request) {
        return staffService.createStaff(storeId, request);
    }

    // =========================================================
    // 🏠 STORE ADDRESS CRUD (Thêm mới)
    // =========================================================

    @Operation(summary = "📋 Lấy danh sách địa chỉ cửa hàng đang đăng nhập")
    @GetMapping("/me/addresses")
    public ResponseEntity<BaseResponse> getAllAddresses() {
        return storeService.getAllAddresses();
    }

    @Operation(summary = "➕ Thêm địa chỉ mới cho cửa hàng đang đăng nhập")
    @PostMapping("/me/addresses")
    public ResponseEntity<BaseResponse> addStoreAddress(
            @Valid @RequestBody StoreAddressRequest request) {
        return storeService.addStoreAddress(request);
    }

    @Operation(summary = "✏️ Cập nhật địa chỉ theo index (của cửa hàng đang đăng nhập)")
    @PutMapping("/me/addresses/{index}")
    public ResponseEntity<BaseResponse> updateStoreAddress(
            @Parameter(description = "Vị trí index của địa chỉ trong danh sách", example = "0")
            @PathVariable int index,
            @Valid @RequestBody StoreAddressRequest request) {
        return storeService.updateStoreAddress(index, request);
    }

    @Operation(summary = "🗑️ Xóa địa chỉ theo index (của cửa hàng đang đăng nhập)")
    @DeleteMapping("/me/addresses/{index}")
    public ResponseEntity<BaseResponse> deleteStoreAddress(
            @Parameter(description = "Vị trí index của địa chỉ trong danh sách", example = "0")
            @PathVariable int index) {
        return storeService.deleteStoreAddress(index);
    }

    @Operation(summary = "🌟 Đặt một địa chỉ làm mặc định (của cửa hàng đang đăng nhập)")
    @PatchMapping("/me/addresses/{index}/default")
    public ResponseEntity<BaseResponse> setDefaultAddress(
            @Parameter(description = "Index của địa chỉ cần đặt làm mặc định", example = "0")
            @PathVariable int index) {
        return storeService.setDefaultAddress(index);
    }

    @Operation(summary = "Danh sách tất cả staff của cửa hàng")
    @GetMapping("/{storeId}/staff")
    public ResponseEntity<BaseResponse> getAllStaff(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var staffList = staffService.getAllStaffByStoreId(storeId);
        var paginated = staffList.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        var response = new BaseResponse<>(
                200,
                "Lấy danh sách staff thành công",
                Map.of(
                        "content", paginated,
                        "total", staffList.size(),
                        "page", page,
                        "size", size
                )
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy chi tiết 1 staff theo storeId và staffId")
    @GetMapping("/{storeId}/staff/{staffId}")
    public ResponseEntity<BaseResponse> getStaffById(
            @PathVariable UUID storeId,
            @PathVariable UUID staffId) {

        StaffResponse staff = staffService.getStaffById(storeId, staffId);
        return ResponseEntity.ok(new BaseResponse<>(200, "Lấy staff thành công", staff));
    }
}
