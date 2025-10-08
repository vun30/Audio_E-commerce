package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService comboService;
    private final StoreRepository storeRepository;

    /**
     * ✅ Hàm tiện ích: Kiểm tra store từ JWT có ở trạng thái ACTIVE hay không
     * - Không trả ra entity
     * - Ném lỗi nếu không đúng store hoặc không active
     */
    private UUID validateActiveStoreAndGetId() {
        // 📩 Lấy principal từ JWT (email[:ROLE])
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        // 🔍 B1: Tìm store theo email
        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        // 🔍 B2: Check trạng thái ACTIVE
        boolean isActive = storeRepository.findByStoreIdAndStatus(store.getStoreId(), StoreStatus.ACTIVE).isPresent();
        if (!isActive) {
            throw new RuntimeException("❌ Store không ở trạng thái ACTIVE. Không thể thao tác.");
        }

        return store.getStoreId();
    }

    /**
     * 🧪 API test kiểm tra trạng thái store từ token
     */
    @GetMapping("/store/me/check")
    public ResponseEntity<BaseResponse> checkMyStoreStatus() {
        validateActiveStoreAndGetId();
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Store đang ở trạng thái ACTIVE", true));
    }

    /**
     * ✅ Tạo combo — chỉ khi store ACTIVE
     */
    @PostMapping
    public ResponseEntity<BaseResponse> createCombo(@RequestBody CreateComboRequest request) {
        UUID storeId = validateActiveStoreAndGetId();
        request.setStoreId(storeId); // ✅ tự động gán storeId từ token
        return comboService.createCombo(request);
    }

    /**
     * 🔎 Lấy combo theo ID (public)
     */
    @GetMapping("/{comboId}")
    public ResponseEntity<BaseResponse> getComboById(@PathVariable UUID comboId) {
        return comboService.getComboById(comboId);
    }

    /**
     * 📦 Lấy tất cả combo (public)
     */
    @GetMapping
    public ResponseEntity<BaseResponse> getAllCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isActive
    ) {
        return comboService.getAllCombos(page, size, keyword, sortDir, minPrice, maxPrice, isActive);
    }

    /**
     * 📦 Lấy tất cả combo của chính store đang login
     */
    @GetMapping("/store/me")
    public ResponseEntity<BaseResponse> getMyCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        UUID storeId = validateActiveStoreAndGetId();
        return comboService.getCombosByStoreId(storeId, page, size, keyword, sortDir, minPrice, maxPrice);
    }

    /**
     * ✏️ Cập nhật combo — chỉ store chủ và store ACTIVE được phép
     */
    @PutMapping("/{comboId}")
    public ResponseEntity<BaseResponse> updateCombo(
            @PathVariable UUID comboId,
            @RequestBody UpdateComboRequest request
    ) {
        validateActiveStoreAndGetId(); // ✅ Kiểm tra quyền trước
        return comboService.updateCombo(comboId, request);
    }

    /**
     * 🛑 Vô hiệu hóa combo — chỉ store chủ và store ACTIVE được phép
     */
    @PutMapping("/{comboId}/disable")
    public ResponseEntity<BaseResponse> disableCombo(@PathVariable UUID comboId) {
        validateActiveStoreAndGetId();
        return comboService.disableCombo(comboId);
    }

    /**
     * 📦 Lấy danh sách sản phẩm trong combo
     */
    @GetMapping("/{comboId}/products")
    public ResponseEntity<BaseResponse> getProductsInCombo(@PathVariable UUID comboId) {
        return comboService.getProductsInCombo(comboId);
    }
}
