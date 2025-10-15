package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Product Combo", description = """
📦 **API Quản lý Combo sản phẩm (Store)**  

**⚙️ Lưu ý cho Frontend:**
- Không cần gửi `storeId` → BE tự lấy từ token người dùng.
- Không cần gửi `categoryId` → BE tự tìm danh mục có tên `"Combo"`.
- FE chỉ cần gửi các thông tin cơ bản (`name`, `comboPrice`, `includedProductIds`, ...).
- Tất cả sản phẩm trong combo phải thuộc **cùng 1 store** và **đang ACTIVE**.
""")
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService comboService;
    private final StoreRepository storeRepository;

    // =========================================================
    // ⚙️ Helper: Kiểm tra store đang login và ACTIVE
    // =========================================================
    private UUID validateActiveStoreAndGetId() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        boolean isActive = storeRepository.findByStoreIdAndStatus(store.getStoreId(), StoreStatus.ACTIVE).isPresent();
        if (!isActive) throw new RuntimeException("❌ Store không ở trạng thái ACTIVE.");

        return store.getStoreId();
    }

    // =========================================================
    // ✅ Tạo combo mới
    // =========================================================
    @Operation(summary = "➕ Tạo combo mới", description = """
        • Chỉ dành cho **Store ACTIVE**.  
        • BE tự động:
          - Gắn `storeId` theo token.
          - Tìm `categoryId` dựa vào tên `"Combo"` trong DB.
        • FE **chỉ cần gửi** các field sau:
          ```json
          {
            "name": "Combo Amp + Loa Hi-End",
            "shortDescription": "Trọn bộ dàn nghe nhạc cao cấp",
            "description": "Gồm 1 amply + 1 cặp loa bookshelf",
            "comboPrice": 35000000,
            "includedProductIds": ["uuid-loa", "uuid-amp"]
          }
          ```
        ⚠️ Không cần gửi `storeId` và `categoryId`.
    """)
    @PostMapping
    public ResponseEntity<BaseResponse> createCombo(@RequestBody CreateComboRequest request) {
        UUID storeId = validateActiveStoreAndGetId();
        request.setStoreId(storeId); // ✅ BE tự gán
        return comboService.createCombo(request);
    }

    // =========================================================
    // ✏️ Cập nhật combo
    // =========================================================
    @Operation(summary = "✏️ Cập nhật combo", description = """
        • Chỉ **store chủ combo** được chỉnh sửa.  
        • BE tự động xử lý `storeId` và `categoryId` ("Combo").  
        • FE chỉ cần gửi field muốn cập nhật:  
          `name`, `description`, `comboPrice`, `includedProductIds`, ...
        • Nếu `includedProductIds` chứa sản phẩm không ACTIVE → từ chối cập nhật.
    """)
    @PutMapping("/{comboId}")
    public ResponseEntity<BaseResponse> updateCombo(
            @PathVariable UUID comboId,
            @RequestBody UpdateComboRequest request
    ) {
        validateActiveStoreAndGetId();
        return comboService.updateCombo(comboId, request);
    }

    // =========================================================
    // 🛑 Disable combo
    // =========================================================
    @Operation(summary = "🛑 Vô hiệu hóa combo", description = """
        • Chỉ **store chủ combo** có thể vô hiệu hóa.  
        • Thay đổi trạng thái `isActive` → `false`.  
        • Không xóa dữ liệu trong DB.  
        • FE chỉ cần gọi: `PUT /api/combos/{comboId}/disable`
    """)
    @PutMapping("/{comboId}/disable")
    public ResponseEntity<BaseResponse> disableCombo(@PathVariable UUID comboId) {
        validateActiveStoreAndGetId();
        return comboService.disableCombo(comboId);
    }

    // =========================================================
    // 🔍 Lấy chi tiết combo (public)
    // =========================================================
    @Operation(summary = "🔍 Lấy chi tiết combo", description = """
        • Trả về toàn bộ thông tin combo, bao gồm danh sách sản phẩm bên trong.  
        • Public — không yêu cầu token.
    """)
    @GetMapping("/{comboId}")
    public ResponseEntity<BaseResponse> getComboById(@PathVariable UUID comboId) {
        return comboService.getComboById(comboId);
    }

    // =========================================================
    // 📜 Lấy danh sách combo (public)
    // =========================================================
    @Operation(summary = "📜 Lấy danh sách combo (public)", description = """
        • Public API, không yêu cầu token.  
        • Có thể lọc theo:
          - `keyword`: tên combo  
          - `minPrice`, `maxPrice`: khoảng giá  
          - `isActive`: trạng thái  
        • Mặc định sắp xếp theo giá tăng (`sortDir=asc`)
    """)
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

    // =========================================================
    // 🏪 Lấy combo của chính store login
    // =========================================================
    @Operation(summary = "🏪 Lấy combo của store đang login", description = """
        • Chỉ store ACTIVE mới gọi được.  
        • BE tự động lấy `storeId` từ token.  
        • Hỗ trợ lọc theo: `keyword`, `minPrice`, `maxPrice`.  
        • FE không cần truyền bất kỳ ID nào.
    """)
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

    // =========================================================
    // 📦 Lấy danh sách sản phẩm trong combo
    // =========================================================
    @Operation(summary = "📦 Lấy danh sách sản phẩm trong combo", description = """
        • Public API.  
        • Trả về danh sách sản phẩm thuộc combo, bao gồm ID & tên sản phẩm.
    """)
    @GetMapping("/{comboId}/products")
    public ResponseEntity<BaseResponse> getProductsInCombo(@PathVariable UUID comboId) {
        return comboService.getProductsInCombo(comboId);
    }
}
