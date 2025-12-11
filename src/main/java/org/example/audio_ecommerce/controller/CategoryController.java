package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateCategoryRequest;
import org.example.audio_ecommerce.dto.request.UpdateCategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ========================================================================================
    // CREATE CATEGORY
    // ========================================================================================
    @Operation(
            summary = "Tạo category mới",
            description = """
                API để tạo danh mục sản phẩm mới.
                
                Hỗ trợ:
                - Category cha/con (parentId)
                - Thuộc tính động (attributes)
                - Thuộc tính có options nếu dataType = SELECT | MULTI_SELECT
                
                FE dùng cho màn Admin -> Quản lý danh mục.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Example Create Category",
                                    value = """
                                    {
                                      "name": "Micro",
                                      "parentId": null,
                                      "attributes": [
                                        {
                                          "attributeName": "polarPattern",
                                          "attributeLabel": "Hướng thu",
                                          "dataType": "SELECT",
                                          "options": ["Cardioid", "Omni", "Super-Cardioid"]
                                        },
                                        {
                                          "attributeName": "maxSPL",
                                          "attributeLabel": "Mức SPL",
                                          "dataType": "NUMBER"
                                        }
                                      ]
                                    }
                                    """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tạo category thành công"
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createCategory(
            @RequestBody CreateCategoryRequest req
    ) {
        return categoryService.createCategory(req);
    }


    // ========================================================================================
    // UPDATE CATEGORY
    // ========================================================================================
    @Operation(
            summary = "Cập nhật category",
            description = """
                API cập nhật thông tin category.
                
                Cho phép:
                - Đổi tên, mô tả, icon, parentId
                - Thêm attribute mới
                - Cập nhật attribute cũ
                - Xóa attribute
                - Cập nhật options (xoá options cũ và thay options mới)
                
                FE dùng cho màn Admin.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Example Update Category",
                                    value = """
                                    {
                                      "name": "Micro Cao Cấp",
                                      "attributesToAdd": [
                                        {
                                          "attributeName": "sensitivity",
                                          "attributeLabel": "Độ nhạy",
                                          "dataType": "NUMBER"
                                        }
                                      ],
                                      "attributesToUpdate": [
                                        {
                                          "attributeId": "a1",
                                          "attributeName": "polarPattern",
                                          "attributeLabel": "Hướng thu",
                                          "dataType": "SELECT",
                                          "options": ["Cardioid", "Omni", "Figure-8"]
                                        }
                                      ],
                                      "attributesToDelete": [
                                        "a2"
                                      ]
                                    }
                                    """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @PutMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> updateCategory(
            @Parameter(description = "ID category cần update")
            @PathVariable UUID categoryId,
            @RequestBody UpdateCategoryRequest req
    ) {
        return categoryService.updateCategory(categoryId, req);
    }


    // ========================================================================================
    // DELETE CATEGORY
    // ========================================================================================
    @Operation(
            summary = "Xoá category",
            description = """
                Xóa category theo ID.
                
                Ràng buộc:
                - Không thể xóa nếu đang có danh mục con
                - Không thể xóa nếu đang có sản phẩm sử dụng category này
                """
    )
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    @ApiResponse(responseCode = "400", description = "Không thể xoá do ràng buộc hệ thống")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> deleteCategory(
            @Parameter(description = "ID category cần xóa")
            @PathVariable UUID categoryId
    ) {
        return categoryService.deleteCategory(categoryId);
    }


    // ========================================================================================
    // GET CATEGORY DETAIL
    // ========================================================================================
    @Operation(
            summary = "Lấy chi tiết category",
            description = """
                Lấy đầy đủ thông tin category bao gồm:
                - Thông tin cơ bản
                - Danh sách thuộc tính (attributes)
                - Danh sách options của từng attribute (nếu có)
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lấy dữ liệu thành công",
            content = @Content(
                    examples = @ExampleObject("""
                    {
                      "status": 200,
                      "message": "Category detail",
                      "data": {
                        "categoryId": "fb4e71a0-bf50-4491-8768-fbbb6820093f",
                        "name": "Micro",
                        "parentId": null,
                        "attributes": [
                          {
                            "attributeId": "a1",
                            "attributeName": "polarPattern",
                            "attributeLabel": "Hướng thu",
                            "dataType": "SELECT",
                            "options": ["Cardioid", "Omni", "Super-Cardioid"]
                          }
                        ]
                      }
                    }
                    """)
            )
    )
    @GetMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> getCategory(
            @PathVariable UUID categoryId
    ) {
        return categoryService.getCategory(categoryId);
    }


    // ========================================================================================
    // GET CATEGORY TREE
    // ========================================================================================
    @Operation(
            summary = "Lấy danh sách category dạng cây",
            description = """
                Category tree:
                - Category cha → con → cháu
                - Phục vụ dropdown chọn danh mục hoặc hiển thị sidebar cây danh mục
                """
    )
    @ApiResponse(responseCode = "200", description = "Lấy thành công")
    @GetMapping("/tree")
    public ResponseEntity<BaseResponse> getCategoryTree() {
        return categoryService.getCategoryTree();
    }

}
