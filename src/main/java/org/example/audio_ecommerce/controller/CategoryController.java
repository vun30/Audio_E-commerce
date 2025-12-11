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

    // ---------------------------------------------------------------------------------------
    // CREATE CATEGORY
    // ---------------------------------------------------------------------------------------
    @Operation(
            summary = "Tạo category mới",
            description = """
                API tạo danh mục mới với các trường:
                - name: Tên category
                - parentId: null nếu là danh mục cha
                - attributes: danh sách thuộc tính kỹ thuật
                
                FE dùng trong màn Admin để thêm danh mục.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload tạo category",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Example Create Category",
                                    value = """
                                    {
                                      "name": "Loa",
                                      "parentId": null,
                                      "attributes": [
                                        {
                                          "attributeName": "frequencyResponse",
                                          "attributeLabel": "Dải tần",
                                          "dataType": "STRING"
                                        },
                                        {
                                          "attributeName": "impedance",
                                          "attributeLabel": "Trở kháng",
                                          "dataType": "STRING"
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
            description = "Tạo category thành công",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Category created successfully",
                      "data": {
                        "categoryId": "fb4e71a0-bf50-4491-8768-fbbb6820093f",
                        "name": "Loa",
                        "parentId": null
                      }
                    }
                    """)
            )
    )
    @PostMapping
    public ResponseEntity<BaseResponse> createCategory(
            @RequestBody CreateCategoryRequest req
    ) {
        return categoryService.createCategory(req);
    }


    // ---------------------------------------------------------------------------------------
    // UPDATE CATEGORY
    // ---------------------------------------------------------------------------------------
    @Operation(
            summary = "Cập nhật category",
            description = """
                API cho phép cập nhật:
                - Tên category
                - parentId
                - Thêm thuộc tính mới
                - Sửa thuộc tính cũ
                - Xoá thuộc tính
                
                FE dùng khi chỉnh sửa thông tin category.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload update category",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Example Update Category",
                                    value = """
                                    {
                                      "name": "Loa Hi-End",
                                      "parentId": null,
                                      "attributesToAdd": [
                                        {
                                          "attributeName": "driverSize",
                                          "attributeLabel": "Kích thước driver",
                                          "dataType": "STRING"
                                        }
                                      ],
                                      "attributesToUpdate": [
                                        {
                                          "attributeId": "ab6dc74d-03d9-4c30-936b-c7f020bef752",
                                          "attributeName": "frequencyResponse",
                                          "attributeLabel": "Dải tần (Hz)",
                                          "dataType": "STRING"
                                        }
                                      ],
                                      "attributesToDelete": [
                                        "73e504c0-9ac4-467e-8f03-dbb30509ff41"
                                      ]
                                    }
                                    """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cập nhật thành công"
    )
    @PutMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> updateCategory(
            @Parameter(description = "ID category cần update")
            @PathVariable UUID categoryId,

            @RequestBody UpdateCategoryRequest req
    ) {
        return categoryService.updateCategory(categoryId, req);
    }


    // ---------------------------------------------------------------------------------------
    // DELETE CATEGORY
    // ---------------------------------------------------------------------------------------
    @Operation(
            summary = "Xoá category",
            description = """
                Xoá category theo ID.
                - Không xoá được nếu đang có sản phẩm sử dụng category này.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Xoá thành công"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Không thể xoá (đang được sử dụng)"
    )
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<BaseResponse> deleteCategory(
            @Parameter(description = "ID category cần xoá")
            @PathVariable UUID categoryId
    ) {
        return categoryService.deleteCategory(categoryId);
    }


    // ---------------------------------------------------------------------------------------
    // GET CATEGORY DETAIL
    // ---------------------------------------------------------------------------------------
    @Operation(
            summary = "Lấy chi tiết category",
            description = "Trả về thông tin danh mục và toàn bộ thuộc tính kỹ thuật."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lấy dữ liệu thành công",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject("""
                    {
                      "status": 200,
                      "message": "Category detail",
                      "data": {
                        "categoryId": "fb4e71a0-bf50-4491-8768-fbbb6820093f",
                        "name": "Loa",
                        "parentId": null,
                        "attributes": [
                          {
                            "attributeId": "ab6dc74d-03d9-4c30-936b-c7f020bef752",
                            "attributeName": "frequencyResponse",
                            "attributeLabel": "Dải tần",
                            "dataType": "STRING"
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


    // ---------------------------------------------------------------------------------------
    // GET CATEGORY TREE
    // ---------------------------------------------------------------------------------------
    @Operation(
            summary = "Lấy danh sách category dạng cây",
            description = """
                API trả về danh mục dạng cây:
                - Danh mục cha → danh mục con → danh mục cháu
                
                FE dùng khi hiển thị bộ lọc category hoặc dropdown phân cấp.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lấy tree thành công",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject("""
                    {
                      "status": 200,
                      "message": "Category tree",
                      "data": [
                        {
                          "categoryId": "id1",
                          "name": "Loa",
                          "children": [
                            {
                              "categoryId": "id2",
                              "name": "Loa Bluetooth",
                              "children": []
                            }
                          ]
                        }
                      ]
                    }
                    """)
            )
    )
    @GetMapping("/tree")
    public ResponseEntity<BaseResponse> getCategoryTree() {
        return categoryService.getCategoryTree();
    }
}
