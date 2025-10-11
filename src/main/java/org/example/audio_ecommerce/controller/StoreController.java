package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreStatusRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Store", description = "Các API quản lý cửa hàng (Admin & Chủ shop)")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Xem chi tiết cửa hàng", description = "Trả về thông tin chi tiết của cửa hàng theo `storeId`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin cửa hàng thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse> getStoreById(
            @Parameter(description = "ID cửa hàng (UUID)", required = true)
            @PathVariable UUID storeId) {
        return storeService.getStoreById(storeId);
    }

    @Operation(summary = "Lấy cửa hàng theo tài khoản", description = "Dùng để lấy cửa hàng của chủ shop dựa vào `accountId`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin cửa hàng thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng cho tài khoản này")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<BaseResponse> getStoreByAccount(
            @Parameter(description = "ID tài khoản (UUID)", required = true)
            @PathVariable UUID accountId) {
        return storeService.getStoreByAccountId(accountId);
    }

    @Operation(
            summary = "Cập nhật thông tin cửa hàng",
            description = "Chủ shop có thể chỉnh sửa các thông tin cơ bản của cửa hàng.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Thông tin cửa hàng cần cập nhật",
                    content = @Content(
                            schema = @Schema(implementation = UpdateStoreRequest.class),
                            examples = @ExampleObject(value = """
                                        {
                                          "storeName": "Loa Nghe Nhạc Cao Cấp",
                                          "description": "Chuyên thiết bị âm thanh Hi-End",
                                          "logoUrl": "https://cdn.example.com/logo.png",
                                          "coverImageUrl": "https://cdn.example.com/cover.jpg",
                                          "address": "123 Nguyễn Trãi, Hà Nội",
                                          "phoneNumber": "0987654321",
                                          "email": "contact@store.vn"
                                        }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse> updateStore(
            @Parameter(description = "ID cửa hàng (UUID)", required = true)
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request) {
        return storeService.updateStore(storeId, request);
    }

    @Operation(
            summary = "Thay đổi trạng thái cửa hàng",
            description = "Admin có thể chuyển trạng thái cửa hàng (`ACTIVE`, `INACTIVE`, `PENDING`, `REJECTED`).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Trạng thái mới cho cửa hàng",
                    content = @Content(
                            schema = @Schema(implementation = UpdateStoreStatusRequest.class),
                            examples = @ExampleObject(value = """
                                        {
                                          "status": "ACTIVE"
                                        }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse> updateStoreStatus(
            @Parameter(description = "ID cửa hàng (UUID)", required = true)
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request) {
        return storeService.updateStoreStatus(storeId, request.getStatus());
    }

    @Operation(
            summary = "Danh sách cửa hàng (phân trang + tìm kiếm)",
            description = """
                    - API trả về danh sách tất cả cửa hàng có hỗ trợ **phân trang** và **tìm kiếm gần đúng theo tên** (giống Google).
                    - Các tham số:
                      - `page`: số trang (mặc định = 0)
                      - `size`: số bản ghi mỗi trang (mặc định = 10)
                      - `keyword`: từ khóa tìm kiếm tên cửa hàng (không bắt buộc)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    @GetMapping
    public ResponseEntity<BaseResponse> getAllStores(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Từ khóa tìm kiếm (không bắt buộc)", example = "loa")
            @RequestParam(required = false) String keyword
    ) {
        return storeService.getAllStores(page, size, keyword);
    }

    @Operation(
            summary = "Lấy thông tin cửa hàng đang đăng nhập",
            description = """
                    API dùng để lấy UUID của cửa hàng hiện đang login.
                    Hệ thống sẽ đọc `email` từ token JWT, sau đó tìm cửa hàng tương ứng.
                    Chỉ dành cho người dùng có role `STOREOWNER`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thành công storeId của cửa hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng cho tài khoản này")
    })
    @GetMapping("/me/id")
    public ResponseEntity<BaseResponse> getMyStoreId() {
        // 🔐 Lấy email từ JWT trong SecurityContext
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        // 🔎 Tìm store theo email
        var storeOpt = storeService.getStoreByEmail(email);
        if (storeOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new BaseResponse<>(404, "❌ Không tìm thấy cửa hàng cho tài khoản: " + email, null));
        }

        // ✅ Trả về storeId
        UUID storeId = storeOpt.get().getStoreId();
        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy storeId thành công", storeId)
        );
    }
}
