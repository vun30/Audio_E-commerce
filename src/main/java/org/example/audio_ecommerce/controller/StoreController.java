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

    @Operation(
            summary = "Admin cập nhật trạng thái cửa hàng",
            description = """
                    API cho phép Admin thay đổi trạng thái hoạt động của cửa hàng.
                    
                    • Các trạng thái hợp lệ:
                        - INACTIVE: Cửa hàng ngừng hoạt động.
                        - PENDING: Đang chờ duyệt KYC.
                        - ACTIVE: Cửa hàng hoạt động bình thường.
                        - REJECTED: KYC bị từ chối.
                        - SUSPENDED: Cửa hàng bị khóa do vi phạm.
                        - PAUSED: Cửa hàng tạm dừng hoạt động.
                    
                    🔥 Hành vi liên quan đến sản phẩm:
                        • Khi chuyển sang SUSPENDED:
                            → Tất cả sản phẩm của cửa hàng sẽ chuyển sang trạng thái SUSPENDED.
                    
                        • Khi chuyển sang PAUSED:
                            → Tất cả sản phẩm của cửa hàng sẽ chuyển sang UNLISTED.
                    
                        • Khi chuyển sang ACTIVE:
                            → Tất cả sản phẩm của cửa hàng sẽ chuyển về ACTIVE.
                    
                    📌 Lưu ý:
                        • FE gửi body JSON chứa trường "status".
                        • API trả về trạng thái mới của cửa hàng và số lượng sản phẩm đã được cập nhật.
                        • Chỉ Admin mới được gọi API này.
                    """
    )
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse> updateStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request
    ) {
        return storeService.updateStoreStatus(storeId, request.getStatus(), request.getReason());
    }

    @Operation(
            summary = "Shop tự đổi trạng thái cửa hàng (ACTIVE <-> PAUSED)",
            description = """
                    API dành cho Chủ Shop tự thay đổi trạng thái cửa hàng.
                    
                    ✔ Cho phép:
                      • ACTIVE  → PAUSED
                      • PAUSED  → ACTIVE
                    
                    ❌ Không cho phép đổi sang các trạng thái khác:
                      • INACTIVE, PENDING, REJECTED, SUSPENDED
                    
                    Hành vi sản phẩm:
                      • Khi PAUSED → toàn bộ sản phẩm UNLISTED
                      • Khi ACTIVE → toàn bộ sản phẩm ACTIVE
                    """
    )
    @PatchMapping("/{storeId}/toggle-status")
    public ResponseEntity<BaseResponse> shopToggleStatus(
            @PathVariable UUID storeId,
            @RequestBody UpdateStoreStatusRequest request) {
        return storeService.shopToggleStoreStatus(storeId, request.getStatus());
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

    @Operation(summary = "✏️ Cập nhật một địa chỉ theo addressId của cửa hàng đang đăng nhập")
    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<BaseResponse> updateStoreAddress(
            @Parameter(description = "UUID của địa chỉ cần cập nhật")
            @PathVariable UUID addressId,
            @Valid @RequestBody StoreAddressRequest request
    ) {
        return storeService.updateStoreAddress(addressId, request);
    }

    @Operation(summary = "🗑️ Xóa một địa chỉ theo addressId của cửa hàng đang đăng nhập")
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<BaseResponse> deleteStoreAddress(
            @Parameter(description = "UUID của địa chỉ cần xoá")
            @PathVariable UUID addressId
    ) {
        return storeService.deleteStoreAddress(addressId);
    }

    @Operation(summary = "🌟 Đặt một địa chỉ làm mặc định theo addressId (của cửa hàng đang đăng nhập)")
    @PatchMapping("/me/addresses/{addressId}/default")
    public ResponseEntity<BaseResponse> setDefaultAddress(
            @Parameter(description = "UUID của địa chỉ cần đặt mặc định")
            @PathVariable UUID addressId
    ) {
        return storeService.setDefaultAddress(addressId);
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

    @GetMapping("/search")
    public ResponseEntity<BaseResponse> searchStores(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return storeService.searchStores(keyword, page, size);
    }

    @Operation(summary = "📦 Lấy địa chỉ mặc định của store dựa vào productId")
    @GetMapping("/address/default-by-product/{productId}")
    public ResponseEntity<BaseResponse<?>> getDefaultAddressByProduct(@PathVariable UUID productId) {
        return storeService.getDefaultAddressByProductId(productId);
    }
}


