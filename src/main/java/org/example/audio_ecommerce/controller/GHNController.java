package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.GhnFeeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Tag(name = "GHN API", description = "Proxy API cho GHN - trả JSON gốc (y hệt Postman)")
public class GHNController {

    private final RestTemplate restTemplate;

    // ✅ Lấy từ application.yml
    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shopId}")
    private String ghnShopId;

    private static final String BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";

    // Hàm tạo Header GHN
    private HttpHeaders createHeaders(boolean includeShopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        if (includeShopId) headers.set("ShopId", ghnShopId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ==========================================================
    // 1️⃣ LẤY DANH SÁCH TỈNH/THÀNH
    // ==========================================================
    @Operation(summary = "Lấy danh sách Tỉnh/Thành", description = "Trả về nguyên JSON từ GHN")
    @GetMapping("/provinces")
    public ResponseEntity<String> getProvinces() {
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(false));
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/master-data/province",
                HttpMethod.GET,
                entity,
                String.class
        );
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
    // 2️⃣ LẤY DANH SÁCH QUẬN/HUYỆN
    // ==========================================================
    @Operation(summary = "Lấy danh sách Quận/Huyện", description = "Body mẫu: { \"province_id\": 202 }")
    @PostMapping("/districts")
    public ResponseEntity<String> getDistricts(@RequestBody String body) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(false));
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/master-data/district",
                HttpMethod.POST,
                entity,
                String.class
        );
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
    // 3️⃣ LẤY DANH SÁCH PHƯỜNG/XÃ
    // ==========================================================
    @Operation(summary = "Lấy danh sách Phường/Xã", description = "Body mẫu: { \"district_id\": 3695 }")
    @PostMapping("/wards")
    public ResponseEntity<String> getWards(@RequestBody String body) {
        String districtId = body.replaceAll("\\D+", ""); // tách số
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(false));
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/master-data/ward?district_id=" + districtId,
                HttpMethod.POST,
                entity,
                String.class
        );
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
    // 4️⃣ TÍNH PHÍ VẬN CHUYỂN
    // ==========================================================

    @Operation(
            summary = "Tính phí dịch vụ (GHN Shipping Fee)",
            description = """
                    🧭 **Mục đích:**  
                    Sử dụng API này để **tính phí dịch vụ trước khi tạo đơn hàng GHN.**

                    ⚠️ **Lưu ý:**  
                    - Cần truyền **Token** và **ShopId** trong header.  
                    - ShopId và Token phải thuộc cùng một tài khoản GHN.  
                    - API có 2 môi trường:  
                      • **Production:** https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee  
                      • **Test:** https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee  

                    🧩 **Ví dụ curl (test):**
                    ```
                    curl --location 'https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee' \\
                    --header 'Content-Type: application/json' \\
                    --header 'Token: <TOKEN>' \\
                    --header 'ShopId: <SHOP_ID>' \\
                    --data '{
                         "service_type_id": 5,
                         "from_district_id": 3695,
                         "from_ward_code": "90768",
                         "to_district_id": 3440,
                         "to_ward_code": "13010",
                         "length": 30,
                         "width": 40,
                         "height": 20,
                         "weight": 3000,
                         "insurance_value": 0,
                         "coupon": null,
                         "items": [
                             {
                                 "name": "TEST1",
                                 "quantity": 1,
                                 "length": 200,
                                 "width": 200,
                                 "height": 200,
                                 "weight": 1000
                             }
                         ]
                     }'
                    ```

                    ---
                    ### 🧱 **Cấu trúc Request**

                    | Trường | Bắt buộc | Kiểu | Mô tả |
                    |---------|-----------|------|-------|
                    | token | ✔ | String | Token GHN dùng để xác thực tài khoản |
                    | shop_id | ✔ | Int | Mã định danh cửa hàng |
                    | service_type_id | ❌ | Int | 2: Hàng nhẹ, 5: Hàng nặng |
                    | insurance_value | ❌ | Int | Giá trị bảo hiểm đơn hàng (tối đa 5.000.000) |
                    | coupon | ❌ | String | Mã giảm giá GHN |
                    | from_district_id | ❌ | Int | Quận/huyện người gửi |
                    | from_ward_code | ❌ | String | Phường/xã người gửi |
                    | to_district_id | ✔ | Int | Quận/huyện người nhận |
                    | to_ward_code | ✔ | String | Phường/xã người nhận |
                    | weight | ✔ | Int | Khối lượng đơn hàng (gram) |
                    | length | ❌ | Int | Chiều dài (cm) |
                    | width | ❌ | Int | Chiều rộng (cm) |
                    | height | ❌ | Int | Chiều cao (cm) |
                    | items | ✔ | Array | Danh sách sản phẩm (bắt buộc với hàng nặng) |
                    | items[].name | ✔ | String | Tên sản phẩm |
                    | items[].quantity | ✔ | Int | Số lượng |
                    | items[].length | ✔ | Int | Chiều dài |
                    | items[].width | ✔ | Int | Chiều rộng |
                    | items[].height | ✔ | Int | Chiều cao |
                    | items[].weight | ✔ | Int | Khối lượng |
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - Trả JSON gốc GHN",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                              "code": 200,
                              "message": "Success",
                              "data": {
                                "total": 36300,
                                "service_fee": 36300,
                                "insurance_fee": 0,
                                "pick_station_fee": 0,
                                "coupon_value": 0,
                                "r2s_fee": 0,
                                "cod_fee": 0
                              }
                            }
                            """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Lỗi: Token hoặc ShopId không hợp lệ (CLIENT_NOT_BELONG_OF_SHOP)")
            }
    )
    @PostMapping("/fee")
    public ResponseEntity<String> calculateFee(@RequestBody GhnFeeRequest request) {
        HttpEntity<GhnFeeRequest> entity = new HttpEntity<>(request, createHeaders(true));
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/shipping-order/fee",
                HttpMethod.POST,
                entity,
                String.class
        );
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }
}
