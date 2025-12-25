package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductViewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/view")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductViewService productViewService;

    // ================================
    // 🖼️ 1) Thumbnail list + Filters
    // ================================
    @Operation(
    summary = "Lấy danh sách sản phẩm dạng thumbnail (có filter, sort, paging)",
    description = """
        API trả về danh sách sản phẩm dạng thumbnail dùng cho trang Homepage, Category, Search.

        🔍 **Bộ lọc hỗ trợ:**
        • status: trạng thái sản phẩm (ACTIVE / INACTIVE)
        • categoryId: lọc theo danh mục
        • storeId: lọc theo cửa hàng
        • keyword: tìm theo tên sản phẩm
                           • provinceCode / districtCode / wardCode: lọc theo địa chỉ cửa hàng
        • minPrice / maxPrice: lọc theo giá
        • minRating: lọc theo số sao tối thiểu
        
        🔄 **Sắp xếp (sorting):**
        • sortBy = name / price
        • sortDir = asc / desc
        
        📄 **Phân trang:**
        • page: số trang (0-based)
        • size: số lượng sản phẩm mỗi trang
        
        📌 **Ví dụ sử dụng (FE):**
        • /products/thumbnails?sortBy=price&sortDir=asc
        • /products/thumbnails?keyword=amply&minPrice=1000000&maxPrice=5000000
        • /products/thumbnails?categoryId=xxx&minRating=4
        """
)
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách sản phẩm thành công"),
        @ApiResponse(responseCode = "400", description = "Request không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
})

    @GetMapping
    public ResponseEntity<BaseResponse> getProductThumbnails(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) String wardCode,

            // 🔥 Filter bổ sung
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minReviewCount,
            @RequestParam(required = false) Integer minViewCount,
            @RequestParam(required = false) Integer minSellCount,

            // Paging
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            // Sorting
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return productViewService.getThumbnailView(
                status,
                categoryId,
                storeId,
                keyword,
                provinceCode,
                districtCode,
                wardCode,
                minPrice,
                maxPrice,
                minRating,
                minReviewCount,
                minViewCount,
                minSellCount,
                pageable,
                sortBy,
                sortDir
        );
    }

    // ================================
    // 🎯 2) PDP – Active vouchers
    // ================================
    @GetMapping("/{productId}/vouchers")
    public ResponseEntity<BaseResponse> getProductVouchers(
            @PathVariable UUID productId,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) String campaignType
    ) {
        return productViewService.getActiveVouchersOfProduct(productId, type, campaignType);
    }


    @Operation(
            summary = "Lấy danh sách sản phẩm gần khách hàng (Nearby products)",
            description = """
        API gợi ý sản phẩm dựa trên vị trí của khách hàng (customer).

        🔍 **Nguyên tắc xác định 'gần':**
        • Lấy `provinceCode` từ **địa chỉ mặc định của customer** (từ token)
        • So khớp với **địa chỉ mặc định của store**
        • KHÔNG cần truyền `storeId`
        • Chỉ lấy sản phẩm có trạng thái **ACTIVE**

        🔎 **Bộ lọc hỗ trợ (giống Thumbnail View):**
        • categoryId: lọc theo danh mục
        • keyword: tìm theo tên / brand / mô tả
        • minPrice / maxPrice: lọc theo giá
        • minRating: số sao tối thiểu
        • minReviewCount / minViewCount / minSellCount

        🔄 **Sắp xếp (sorting):**
        • sortBy = name | price | rating | sell | view | review
        • sortDir = asc | desc

        📄 **Phân trang (paging):**
        • page: số trang (0-based)
        • size: số lượng sản phẩm mỗi trang

        📌 **Ghi chú quan trọng:**
        • Nếu customer chưa có địa chỉ mặc định → API trả 400
        • districtCode / wardCode hiện KHÔNG áp dụng (chỉ so theo tỉnh)

        📌 **Ví dụ:**
        • /api/products/view/nearby
        • /api/products/view/nearby?keyword=loa&minPrice=1000000
        """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách sản phẩm gần thành công"),
            @ApiResponse(responseCode = "400", description = "Customer chưa có địa chỉ mặc định hoặc request không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    @GetMapping("/nearby")
    public ResponseEntity<BaseResponse> getNearbyThumbnails(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,

            // ✅ filter thêm giống thumbnail
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minReviewCount,
            @RequestParam(required = false) Integer minViewCount,
            @RequestParam(required = false) Integer minSellCount,

            // paging + sorting
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return productViewService.getNearbyThumbnailView(
                status,
                categoryId,
                storeId,
                keyword,
                minPrice,
                maxPrice,
                minRating,
                minReviewCount,
                minViewCount,
                minSellCount,
                pageable,
                sortBy,
                sortDir
        );
    }

}
