package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductSpecUpsertRequest;
import org.example.audio_ecommerce.dto.request.SimilarProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductSpecService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSpecController {

    private final ProductSpecService productSpecService;

//    // =========================================================
//    // UPSERT THÔNG SỐ KỸ THUẬT SẢN PHẨM
//    // =========================================================
//    @Operation(
//            summary = "Cập nhật / thêm mới thông số kỹ thuật sản phẩm",
//            description = """
//                API dùng để lưu thông số kỹ thuật (THÔNG SỐ KỸ THUẬT) cho sản phẩm.
//
//                ✔ FE gửi dữ liệu theo DTO ThongSoKyThuat (tiếng Việt)
//                ✔ BE tự map sang attribute tiếng Anh trong DB
//                ✔ Giá trị NUMBER sẽ được parse số (ví dụ: '50 Hz' → 50)
//
//                Category:
//                - Có thể truyền categoryId
//                - Hoặc categoryName
//                - Nếu không truyền → BE xử lý theo logic mặc định (hiện tại là Loa)
//
//                API này có thể gọi nhiều lần (UPSERT):
//                - Nếu attribute đã tồn tại → update
//                - Nếu chưa tồn tại → insert
//                """
//    )
//    @PutMapping("/{productId}/spec")
//    public ResponseEntity<BaseResponse<Map<String, UUID>>> upsertSpec(
//
//            @Parameter(
//                    description = "ID sản phẩm cần cập nhật thông số kỹ thuật",
//                    example = "4fdb628f-c240-4274-8458-b4797142799a",
//                    required = true
//            )
//            @PathVariable UUID productId,
//
//            @Parameter(
//                    description = "ID category (OPTIONAL). Nếu không truyền, BE dùng category mặc định",
//                    example = "cbe6d863-8108-4f3d-a7ce-385dbae5022a"
//            )
//            @RequestParam(required = false) UUID categoryId,
//
//            @Parameter(
//                    description = "Tên category (OPTIONAL). Dùng khi không có categoryId",
//                    example = "Loa"
//            )
//            @RequestParam(required = false) String categoryName,
//
//            @RequestBody(
//                    description = """
//                        DTO chứa thông số kỹ thuật sản phẩm (tiếng Việt).
//
//                        Ví dụ:
//                        {
//                          "thongSoKyThuat": {
//                            "daiTanSo": {
//                              "tanSoThap": "50 Hz",
//                              "tanSoCao": "20000 Hz"
//                            },
//                            "congSuat": "100W",
//                            "troKhang": "8Ω",
//                            "doNhay": "90 dB/W/m",
//                            "doMeoTieng": "0.5%",
//                            "tanSoCrossover": "2000 Hz"
//                          }
//                        }
//                        """
//            )
//            @org.springframework.web.bind.annotation.RequestBody ProductSpecUpsertRequest req
//    ) {
//        productSpecService.upsertSpec(productId, categoryId, categoryName, req);
//
//        return ResponseEntity.ok(
//                BaseResponse.success(
//                        "✅ Cập nhật thông số kỹ thuật thành công",
//                        Map.of("productId", productId)
//                )
//        );
//    }

    // =========================================================
    // SEARCH SẢN PHẨM TƯƠNG TỰ THEO THÔNG SỐ KỸ THUẬT
    // =========================================================
    @Operation(
            summary = "Tìm sản phẩm tương tự theo thông số kỹ thuật",
            description = """
                API dùng để tìm các sản phẩm có thông số kỹ thuật GẦN GIỐNG với input.
                
                ✔ FE chỉ cần gửi thông số kỹ thuật (không cần categoryId)
                ✔ Category mặc định hiện tại: Loa
                ✔ Chỉ so sánh các attribute dạng NUMBER
                ✔ Áp dụng tolerance ±10% cho mỗi thông số
                
                Công thức:
                - Mỗi thông số được chấm điểm từ 0 → 1
                - Điểm cuối = trung bình các thông số match
                - Trả về danh sách productId theo độ tương đồng giảm dần
                
                Phù hợp cho:
                - Gợi ý sản phẩm tương tự
                - So sánh cấu hình
                - AI recommendation đơn giản
                """
    )
    @PostMapping("/similar/spec")
    public ResponseEntity<BaseResponse<Map<String, List<UUID>>>> searchSimilar(

            @RequestBody(
                    description = """
                        DTO chứa thông số kỹ thuật cần tìm sản phẩm tương tự.
                        
                        Ví dụ:
                        {
                          "topN": 10,
                          "thongSoKyThuat": {
                            "daiTanSo": {
                              "tanSoThap": "50 Hz",
                              "tanSoCao": "800000 Hz"
                            },
                            "congSuat": "40W",
                            "troKhang": "80Ω",
                            "doNhay": "80 dB/W/m",
                            "doMeoTieng": "80%",
                            "tanSoCrossover": "80 Hz"
                          }
                        }
                        """
            )
            @org.springframework.web.bind.annotation.RequestBody SimilarProductRequest req
    ) {
        List<UUID> ids = productSpecService.searchSimilar(req);

        return ResponseEntity.ok(
                BaseResponse.success(
                        "✅ Danh sách sản phẩm tương tự",
                        Map.of("productIds", ids)
                )
        );
    }
}
