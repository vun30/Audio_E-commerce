package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "📦 Product API", description = "Quản lý sản phẩm dành cho Admin & Store")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ============================================================
    // 📜 GET: Danh sách sản phẩm (filter + pagination)
    // ============================================================
    @GetMapping
@Operation(
        summary = "📜 Lấy danh sách sản phẩm",
        description = """
                • Lọc theo: danh mục, store, từ khóa, trạng thái sản phẩm.  
                • Lọc theo khoảng giá: áp dụng cho cả giá sản phẩm và giá thấp nhất của biến thể.  
                • Hỗ trợ phân trang & sắp xếp theo ngày tạo (mới nhất trước).  
                • Trả về danh sách `ProductResponse`.  
                """
)
public ResponseEntity<BaseResponse> getAllProducts(

        // ===========================
        // 🎯 Lọc theo danh mục
        // ===========================
        @Parameter(
                description = "Tên danh mục (lọc). VD: Loa, Tai Nghe, DAC, Mixer...",
                example = "Loa"
        )
        @RequestParam(required = false) String categoryName,

        // ===========================
        // 🏪 Lọc theo store (UUID)
        // ===========================
        @Parameter(
                description = """
                        UUID của Store (lọc theo cửa hàng).  
                        Nếu để rỗng → không lọc theo store.  
                        """,
                example = "b57e964c-2cf1-4ca7-9e8a-82d27d0cbe11"
        )
        @RequestParam(required = false) String storeId,

        // ===========================
        // 🔍 Lọc theo tên sản phẩm
        // ===========================
        @Parameter(
                description = "Tìm kiếm theo tên sản phẩm (keyword). Không phân biệt hoa thường.",
                example = "sony"
        )
        @RequestParam(required = false) String keyword,

        // ===========================
        // 📄 Pagination
        // ===========================
        @Parameter(description = "Trang hiện tại", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Số phần tử mỗi trang", example = "20")
        @RequestParam(defaultValue = "20") int size,

        // ===========================
        // 📌 Lọc theo trạng thái sản phẩm
        // ===========================
        @Parameter(
                description = "Trạng thái sản phẩm",
                schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE", "DISCONTINUED"}),
                example = "ACTIVE"
        )
        @RequestParam(required = false) ProductStatus status,

        // ===========================
        // 💰 Lọc theo giá tối thiểu
        // ===========================
        @Parameter(description = "Giá thấp nhất cần lọc (áp dụng cho cả variant)", example = "500000")
        @RequestParam(required = false) BigDecimal minPrice,

        // ===========================
        // 💰 Lọc theo giá tối đa
        // ===========================
        @Parameter(description = "Giá cao nhất cần lọc (áp dụng cho cả variant)", example = "2000000")
        @RequestParam(required = false) BigDecimal maxPrice
) {

    // ======================================================
    // 🛠️ KIỂM TRA & CHUYỂN storeId từ String → UUID
    // ======================================================
    UUID storeUUID = null;

    if (storeId != null && !storeId.isBlank()) {
        try {
            storeUUID = UUID.fromString(storeId.trim());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.error("❌ storeId không đúng định dạng UUID")
            );
        }
    }

    // ======================================================
    // 🚀 CALL SERVICE
    // ======================================================
    return productService.getAllProducts(
            categoryName,
            storeUUID,
            keyword,
            page,
            size,
            status,
            minPrice,
            maxPrice
    );
}


    // ============================================================
    // 🔎 GET: Chi tiết sản phẩm
    // ============================================================
    @Operation(
            summary = "🔎 Xem chi tiết sản phẩm",
            description = "Trả về thông tin đầy đủ của sản phẩm theo productId."
    )
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse> getProductById(
            @Parameter(description = "UUID sản phẩm", example = "8e7e26a8-2b2a-4bc5-a617-40a9e2a6f1f0")
            @PathVariable UUID productId
    ) {
        return productService.getProductById(productId);
    }

    // ============================================================
    // ➕ POST: Tạo sản phẩm
    // ============================================================
    @Operation(
            summary = "➕ Tạo sản phẩm mới (Store)",
            description = """
                    • API chỉ dành cho Store đã đăng nhập.  
                    • `storeId` auto mapping theo user login.  
                    • `slug` tự sinh từ tên sản phẩm.  
                    • SKU phải duy nhất trong một store.  
                    • Trả về thông tin sản phẩm sau khi tạo.  
                    """
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createProduct(
            @Parameter(description = "Dữ liệu tạo sản phẩm mới")
            @RequestBody ProductRequest request
    ) {
        return productService.createProduct(request);
    }

    // ============================================================
    // ✏️ PUT: Cập nhật sản phẩm
    // ============================================================
    @Operation(
            summary = "✏️ Cập nhật sản phẩm",
            description = """
                    • Chỉ store sở hữu sản phẩm mới có quyền cập nhật.  
                    • Các trường null sẽ giữ nguyên.  
                    • Nếu đổi tên → slug tự cập nhật.  
                    • Nếu đổi categoryName → BE tự map.  
                    """
    )
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> updateProduct(
            @Parameter(description = "UUID sản phẩm cần update", example = "13e1be55-8c60-4135-af8e-732c10c81397")
            @PathVariable UUID productId,

            @Parameter(description = "Dữ liệu cập nhật sản phẩm")
            @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(productId, request);
    }

    // ============================================================
    // 🚫 DELETE: Vô hiệu hóa sản phẩm
    // ============================================================
    @Operation(
            summary = "🚫 Vô hiệu hóa sản phẩm",
            description = """
                    • Không xóa khỏi DB.  
                    • Chỉ đổi trạng thái sang INACTIVE.  
                    • Dùng khi shop muốn tạm ẩn sản phẩm.  
                    """
    )
    @DeleteMapping("/{productId}")
    public ResponseEntity<BaseResponse> disableProduct(
            @Parameter(description = "UUID sản phẩm muốn vô hiệu hóa")
            @PathVariable UUID productId
    ) {
        return productService.disableProduct(productId);
    }

    // ============================================================
    // 👁️ POST: Tăng lượt xem sản phẩm
    // ============================================================
    @Operation(
            summary = "👁️ Tăng lượt xem sản phẩm",
            description = """
                    • API công khai, tăng viewCount của sản phẩm lên 1.  
                    • Gọi khi user xem chi tiết sản phẩm.  
                    • Trả về productId và viewCount mới.  
                    """
    )
    @PostMapping("/{productId}/view")
    public ResponseEntity<BaseResponse> incrementViewCount(
            @Parameter(description = "UUID sản phẩm", example = "8e7e26a8-2b2a-4bc5-a617-40a9e2a6f1f0")
            @PathVariable UUID productId
    ) {
        return productService.incrementViewCount(productId);
    }
}
