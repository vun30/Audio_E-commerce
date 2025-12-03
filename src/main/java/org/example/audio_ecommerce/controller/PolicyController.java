package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.PolicyCategoryRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemUpdateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.PolicyCategoryResponse;
import org.example.audio_ecommerce.dto.response.PolicyItemResponse;
import org.example.audio_ecommerce.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
    name = "📋 Policy Management API",
    description = """
        ## API quản lý Chính sách & Điều khoản (Flexible - ALL PUBLIC)
        
        ### 🎯 Dành cho Frontend:
        - ✅ **TẤT CẢ APIs đều PUBLIC** - Không cần authentication
        - ✅ **Không cần phân quyền** - Ai cũng có thể CRUD
        
        ### ⚡ Đặc điểm:
        - ✅ **Không giới hạn số lượng danh mục** - Tạo bao nhiêu cũng được
        - ✅ **Tên danh mục tự do** - Không ràng buộc, đặt tên gì cũng được
        - ✅ **Mỗi danh mục có nhiều mục con** (Policy Items)
        - ✅ **Hỗ trợ text dài và nhiều ảnh** cho mỗi mục con
        - ✅ **Open API** - Không cần token, không cần login
        
        ### 📝 Use Cases:
        - Chính sách bảo mật, Điều khoản sử dụng
        - Hướng dẫn thanh toán, vận chuyển, đổi trả
        - FAQ, Giới thiệu, Liên hệ
        - Bất kỳ nội dung tĩnh nào khác
        
        ### 🔗 Base URL: `/api/policies`
        
        ### 📊 Workflow:
        1. **Tạo danh mục** → POST /categories (tên tự do)
        2. **Tạo mục con** → POST /items (với categoryId)
        3. **Get danh sách** → GET /categories
        4. **Get chi tiết** → GET /categories/{id} (kèm items)
        """
)
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    // ============= POLICY CATEGORY CRUD (ALL PUBLIC) =============

    @Operation(
        summary = "🏷️ [PUBLIC] Tạo danh mục mới",
        description = """
            ### Tạo danh mục chính sách/điều khoản mới - KHÔNG GIỚI HẠN
            
            ⚡ **Tạo thoải mái** - Không giới hạn số lượng danh mục
            
            **Request Body Example:**
            ```json
            {
              "name": "Chính sách bảo mật",
              "description": "Chính sách bảo mật thông tin khách hàng",
              "iconUrl": "https://cdn.example.com/icons/privacy.png",
              "displayOrder": 1,
              "isActive": true
            }
            ```
            
            **Các ví dụ tên danh mục:**
            - "Chính sách bảo mật"
            - "Điều khoản sử dụng"
            - "Hướng dẫn thanh toán"
            - "Chính sách vận chuyển"
            - "Giới thiệu về chúng tôi"
            - "Liên hệ"
            - "FAQ"
            - ... (Bất kỳ tên nào)
            
            📌 **Sau khi tạo danh mục, dùng ID để tạo các mục con**
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "✅ Tạo thành công"),
        @ApiResponse(responseCode = "400", description = "❌ Validation error")
    })
    @PostMapping("/categories")
    public ResponseEntity<BaseResponse> createPolicyCategory(@Valid @RequestBody PolicyCategoryRequest request) {
        log.info("REST request to create policy category: {}", request.getName());
        PolicyCategoryResponse response = policyService.createPolicyCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Tạo danh mục thành công")
                        .data(response)
                        .build());
    }

    @Operation(
        summary = "✏️ [PUBLIC] Cập nhật danh mục",
        description = "Cập nhật thông tin danh mục. Request body giống như POST."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "✅ Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy danh mục")
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<BaseResponse> updatePolicyCategory(
            @Parameter(description = "UUID của danh mục") @PathVariable UUID id,
            @Valid @RequestBody PolicyCategoryRequest request) {
        log.info("REST request to update policy category ID: {}", id);
        PolicyCategoryResponse response = policyService.updatePolicyCategory(id, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật danh mục thành công")
                .data(response)
                .build());
    }

    @Operation(
        summary = "🔎 [PUBLIC] Lấy chi tiết danh mục + items",
        description = """
            ### Lấy danh mục kèm TẤT CẢ mục con
            
            **Response:**
            ```json
            {
              "status": 200,
              "data": {
                "id": "uuid",
                "name": "Chính sách bảo mật",
                "description": "...",
                "policyItems": [
                  {
                    "id": "item-1",
                    "title": "1. Thu thập thông tin",
                    "content": "Nội dung chi tiết...",
                    "displayOrder": 1,
                    "imageUrls": ["url1", "url2"]
                  },
                  { ... }
                ]
              }
            }
            ```
            
            ✅ **Frontend Usage:**
            - Render trang chi tiết chính sách
            - Loop qua `policyItems` array
            - Hiển thị theo `displayOrder`
            - Render images từ `imageUrls`
            
            📌 **1 API call = đủ data để render trang**
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "✅ Thành công"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy danh mục")
    })
    @GetMapping("/categories/{id}")
    public ResponseEntity<BaseResponse> getPolicyCategoryById(
            @Parameter(description = "UUID của danh mục") @PathVariable UUID id) {
        log.info("REST request to get policy category by ID: {}", id);
        PolicyCategoryResponse response = policyService.getPolicyCategoryById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin danh mục thành công")
                .data(response)
                .build());
    }


    @Operation(
        summary = "📜 [PUBLIC] Lấy tất cả danh mục",
        description = """
            ### Lấy danh sách tất cả danh mục (KHÔNG bao gồm items)
            
            **Response:**
            ```json
            {
              "status": 200,
              "data": [
                {
                  "id": "uuid-1",
                  "name": "Chính sách bảo mật",
                  "description": "...",
                  "iconUrl": "...",
                  "displayOrder": 1,
                  "itemCount": 4
                },
                { ... }
              ]
            }
            ```
            
            ✅ **Frontend Usage:**
            - Hiển thị menu navigation
            - Sidebar links
            - Footer sitemap
            
            💡 **Sắp xếp:** Theo `displayOrder` ASC
            
            📌 **Lấy từng ID để gọi GET /categories/{id} lấy nội dung chi tiết**
            """
    )
    @ApiResponse(responseCode = "200", description = "✅ Thành công - Trả về list rỗng nếu không có data")
    @GetMapping("/categories")
    public ResponseEntity<BaseResponse> getAllPolicyCategories() {
        log.info("REST request to get all policy categories");
        List<PolicyCategoryResponse> response = policyService.getAllPolicyCategories();
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách danh mục thành công")
                .data(response)
                .build());
    }

    @Operation(
        summary = "🗑️ [PUBLIC] Xóa danh mục chính sách",
        description = """
            ### Xóa mềm danh mục (soft delete)
            
            ⚠️ **Lưu ý:** 
            - Xóa mềm (set `isActive = false`)
            - Tất cả mục con cũng bị xóa mềm
            - Dữ liệu vẫn còn trong DB
            - Frontend không còn thấy trong API public
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "✅ Xóa thành công"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy")
    })

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<BaseResponse> deletePolicyCategory(
            @Parameter(description = "UUID của danh mục cần xóa") @PathVariable UUID id) {
        log.info("REST request to delete policy category ID: {}", id);
        policyService.deletePolicyCategory(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Xóa danh mục thành công")
                .build());
    }

    // ============= POLICY ITEM CRUD (ALL PUBLIC) =============

    @Operation(
        summary = "➕ [PUBLIC] Tạo mục con trong danh mục",
        description = """
            ### Tạo mục con mới cho danh mục
            
            **Request Body Example:**
            ```json
            {
              "policyCategoryId": "550e8400-e29b-41d4-a716-446655440000",
              "title": "1. Thu thập thông tin",
              "content": "Chúng tôi thu thập:\\n- Họ tên\\n- Email\\n- SĐT",
              "displayOrder": 1,
              "imageUrls": [
                "https://cdn.example.com/image1.png",
                "https://cdn.example.com/image2.png"
              ],
              "isActive": true
            }
            ```
            
            📝 **Notes:**
            - `content` hỗ trợ text dài (TEXT type)
            - `imageUrls` là array, có thể rỗng []
            - `\\n` trong content = line break
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "✅ Tạo thành công"),
        @ApiResponse(responseCode = "400", description = "❌ Validation error"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy category")
    })

    @PostMapping("/items")
    public ResponseEntity<BaseResponse> createPolicyItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin mục con cần tạo",
                required = true
            )
            @Valid @RequestBody PolicyItemRequest request) {
        log.info("REST request to create policy item for category: {}", request.getPolicyCategoryId());
        PolicyItemResponse response = policyService.createPolicyItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Tạo mục thành công")
                        .data(response)
                        .build());
    }

    @Operation(
        summary = "✏️ [PUBLIC] Cập nhật mục con",
        description = "Cập nhật thông tin mục con. Request body giống POST nhưng không cần `policyCategoryId`"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "✅ Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy item")
    })


    @PutMapping("/items/{id}")
    public ResponseEntity<BaseResponse> updatePolicyItem(
            @Parameter(description = "UUID của mục con cần cập nhật") @PathVariable UUID id,
            @Valid @RequestBody PolicyItemUpdateRequest request) {
        log.info("REST request to update policy item ID: {}", id);
        PolicyItemResponse response = policyService.updatePolicyItem(id, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật mục thành công")
                .data(response)
                .build());
    }

    @Operation(
        summary = "🔎 [PUBLIC] Lấy chi tiết mục con theo ID",
        description = """
            ### Lấy thông tin chi tiết một mục con
            
            **Response bao gồm:**
            - title, content
            - imageUrls array
            - policyCategoryId và policyCategoryName
            - timestamps
            """
    )
    @ApiResponse(responseCode = "200", description = "✅ Thành công")
    @GetMapping("/items/{id}")
    public ResponseEntity<BaseResponse> getPolicyItemById(
            @Parameter(description = "UUID của mục con") @PathVariable UUID id) {
        log.info("REST request to get policy item by ID: {}", id);
        PolicyItemResponse response = policyService.getPolicyItemById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin mục thành công")
                .data(response)
                .build());
    }

    @Operation(
        summary = "📋 [PUBLIC] Lấy danh sách mục con theo danh mục",
        description = """
            ### Lấy tất cả mục con của một danh mục
            
            **Response:** Array of items, sắp xếp theo `displayOrder`
            
            💡 **Tip:** Thường dùng GET `/categories/{id}` sẽ tốt hơn 
            vì trả về cả category + items trong 1 request
            """
    )
    @ApiResponse(responseCode = "200", description = "✅ Thành công - Trả về [] nếu không có items")
    @GetMapping("/categories/{categoryId}/items")
    public ResponseEntity<BaseResponse> getPolicyItemsByCategoryId(
            @Parameter(description = "UUID của danh mục") @PathVariable UUID categoryId) {
        log.info("REST request to get policy items for category ID: {}", categoryId);
        List<PolicyItemResponse> response = policyService.getPolicyItemsByCategoryId(categoryId);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách mục thành công")
                .data(response)
                .build());
    }

    @Operation(
        summary = "🗑️ [PUBLIC] Xóa mục con",
        description = "Xóa mềm mục con (soft delete - set `isActive = false`)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "✅ Xóa thành công"),
        @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy")
    })

    @DeleteMapping("/items/{id}")
    public ResponseEntity<BaseResponse> deletePolicyItem(
            @Parameter(description = "UUID của mục con cần xóa") @PathVariable UUID id) {
        log.info("REST request to delete policy item ID: {}", id);
        policyService.deletePolicyItem(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Xóa mục thành công")
                .build());
    }

}


