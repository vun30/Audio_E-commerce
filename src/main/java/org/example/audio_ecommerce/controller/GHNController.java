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

  // ==========================================================
// 5️⃣ TẠO ĐƠN HÀNG GHN (CREATE ORDER)
// ==========================================================
@Operation(
        summary = "📦 Tạo đơn hàng mới trên GHN (Create Order)",
        description = """
                🧭 **Mục đích:**  
                Gửi toàn bộ thông tin đơn hàng sang hệ thống GHN để khởi tạo vận đơn.  
                Trả về mã đơn GHN (`order_code`), thời gian giao dự kiến (`expected_delivery_time`), phí (`total_fee`) và chi tiết các loại phí.

                ⚠️ **Lưu ý quan trọng:**  
                - Cần truyền **`Token`** và **`ShopId`** trong Header (BE tự động chèn từ `application.yml`).  
                - GHN chia 2 môi trường:  
                  • **Production:** https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create  
                  • **Test:** https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create  
                - Một số trường hợp **Token và ShopId không khớp tài khoản GHN** sẽ trả lỗi `"CLIENT_NOT_BELONG_OF_SHOP"`.  
                - Đơn chỉ được tạo nếu địa chỉ (district/ward) hợp lệ — nên lấy từ API `/districts` và `/wards` của GHN trước khi tạo đơn.

                ---

                ### 🧱 **Nhóm trường bắt buộc (Required Fields)**

                #### 🏬 **FROM - Thông tin người gửi (Shop)**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | from_name | ✔ | Tên shop gửi |
                | from_phone | ✔ | Số điện thoại người gửi |
                | from_address | ✔ | Địa chỉ đầy đủ người gửi |
                | from_ward_name | ✔ | Tên phường/xã người gửi |
                | from_district_name | ✔ | Tên quận/huyện người gửi |
                | from_province_name | ✔ | Tên tỉnh/thành phố người gửi |

                #### 📦 **TO - Thông tin người nhận (Customer)**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | to_name | ✔ | Tên khách nhận |
                | to_phone | ✔ | Số điện thoại khách nhận |
                | to_address | ✔ | Địa chỉ nhận hàng đầy đủ |
                | to_ward_code | ✔ | Mã phường/xã người nhận (lấy từ API `/wards`) |
                | to_district_id | ✔ | Mã quận/huyện người nhận (lấy từ API `/districts`) |

                #### 💰 **COD & Thanh toán**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | cod_amount | ❌ | Số tiền cần thu hộ (tối đa 10.000.000 VND) |
                | payment_type_id | ✔ | 1: Shop trả phí ship, 2: Người nhận trả |
                | insurance_value | ❌ | Giá trị khai báo bảo hiểm (≤ 5.000.000 VND) |

                #### ⚙️ **Dịch vụ & Kích thước kiện hàng**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | service_type_id | ✔ | 1: Express, 2: Standard |
                | service_id | ❌ | ID dịch vụ cụ thể (nếu có) |
                | weight | ✔ | Trọng lượng hàng (gram, ≤ 30.000g) |
                | length | ✔ | Chiều dài (cm, ≤ 150cm) |
                | width | ✔ | Chiều rộng (cm, ≤ 150cm) |
                | height | ✔ | Chiều cao (cm, ≤ 150cm) |

                #### 📝 **Ghi chú & Cài đặt thêm**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | required_note | ✔ | `CHOTHUHANG`, `CHOXEMHANGKHONGTHU`, `KHONGCHOXEMHANG` |
                | note | ❌ | Ghi chú cho shipper, ví dụ: "Gọi trước khi giao" |
                | pick_shift | ❌ | Mảng ID ca lấy hàng, lấy từ `/v2/shift/date` |
                | coupon | ❌ | Mã giảm giá GHN (nếu có) |

                #### 🧾 **Danh sách sản phẩm (Items[])**
                | Trường | Bắt buộc | Mô tả |
                |--------|-----------|------|
                | name | ✔ | Tên sản phẩm |
                | code | ❌ | Mã SKU sản phẩm |
                | quantity | ✔ | Số lượng |
                | price | ❌ | Giá bán |
                | weight | ✔ | Trọng lượng sản phẩm |
                | length | ❌ | Dài (cm) |
                | width | ❌ | Rộng (cm) |
                | height | ❌ | Cao (cm) |
                | category.level1 | ❌ | Nhóm sản phẩm cấp 1 (Audio, Phụ kiện,...) |

                ---

                ### 📤 **Response mẫu**
                ```json
                {
                  "code": 200,
                  "message": "Success",
                  "data": {
                    "order_code": "FFFNL9HH",
                    "expected_delivery_time": "2025-11-12T16:00:00Z",
                    "total_fee": 33000,
                    "fee": {
                      "main_service": 22000,
                      "insurance": 11000,
                      "station_do": 0,
                      "station_pu": 0
                    }
                  }
                }
                ```

                ---

                ### ❌ **Response lỗi ví dụ**
                ```json
                {
                  "code": 400,
                  "message": "Sai thông tin Required Note hoặc địa chỉ không hợp lệ",
                  "code_message": "USER_ERR_COMMON",
                  "data": null
                }
                ```
                """,
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Thành công - Đơn GHN đã được tạo",
                        content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                {
                                  "code": 200,
                                  "message": "Success",
                                  "data": {
                                    "order_code": "FFFNL9HH",
                                    "expected_delivery_time": "2025-11-12T16:00:00Z",
                                    "total_fee": 33000
                                  }
                                }
                                """))
                ),
                @ApiResponse(responseCode = "400", description = "Sai thông tin hoặc thiếu field bắt buộc")
        }
)
@PostMapping("/create-order")
public ResponseEntity<String> createOrder(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Body JSON chứa đầy đủ thông tin người gửi, người nhận, kích thước, trọng lượng và danh sách sản phẩm",
                required = true,
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = org.example.audio_ecommerce.dto.request.GhnCreateOrderRequest.class))
        )
        @RequestBody org.example.audio_ecommerce.dto.request.GhnCreateOrderRequest request
) {
    HttpEntity<org.example.audio_ecommerce.dto.request.GhnCreateOrderRequest> entity =
            new HttpEntity<>(request, createHeaders(true));

    ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/v2/shipping-order/create",
            HttpMethod.POST,
            entity,
            String.class
    );

    return ResponseEntity.status(response.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(response.getBody());
}

    // ==========================================================
// 6️⃣ HỦY ĐƠN HÀNG GHN (CANCEL ORDER)
// ==========================================================
    @Operation(
            summary = "🚫 Hủy đơn hàng GHN (Cancel Order)",
            description = """
                    🧭 **Mục đích:**  
                    Hủy đơn hàng GHN khi đơn chưa được lấy (chưa sang trạng thái đang giao).  
                    Cần gửi `order_codes` (mã vận đơn GHN) trong body JSON.
                    
                    ⚠️ **Lưu ý:**  
                    - BE tự động truyền `Token` và `ShopId` từ file cấu hình `application.yml`.  
                    - GHN chỉ cho phép hủy khi đơn hàng ở trạng thái **mới tạo / chờ lấy hàng**.  
                    - Sau khi huỷ thành công, GHN trả về `result = true`.
                    
                    ---
                    ### 🧱 **Ví dụ request:**
                    ```json
                    { "order_codes": ["GY6MKWB6"] }
                    ```
                    
                    ---
                    ### 📤 **Response mẫu thành công:**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": [
                        {
                          "order_code": "GY6MKWB6",
                          "result": true,
                          "message": "OK"
                        }
                      ]
                    }
                    ```
                    
                    ---
                    ### ⚠️ **Response lỗi (ví dụ):**
                    ```json
                    {
                      "code": 400,
                      "message": "code=400, message=Syntax error: invalid request body",
                      "data": null,
                      "code_message": "USER_ERR_COMMON"
                    }
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - Đơn hàng đã hủy trên GHN",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": [
                                        {
                                          "order_code": "GY6MKWB6",
                                          "result": true,
                                          "message": "OK"
                                        }
                                      ]
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Sai định dạng hoặc đơn hàng không thể hủy")
            }
    )
    @PostMapping("/cancel-order")
    public ResponseEntity<String> cancelOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Body chứa danh sách mã đơn hàng GHN cần hủy (`order_codes`)",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                                    {
                                      "order_codes": ["GY6MKWB6", "FFTEST22"]
                                    }
                                    """))
            )
            @RequestBody String body
    ) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(true));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/switch-status/cancel",
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
// 7️⃣ TRA CỨU CHI TIẾT ĐƠN HÀNG GHN (ORDER DETAIL)
// ==========================================================
    @Operation(
            summary = "🔍 Tra cứu chi tiết đơn hàng GHN (Order Info)",
            description = """
                    🧭 **Mục đích:**  
                    Lấy toàn bộ thông tin của 1 đơn hàng GHN, bao gồm:
                    - Thông tin người gửi, người nhận  
                    - Kích thước, trọng lượng, COD, bảo hiểm  
                    - Trạng thái hiện tại (`status`)  
                    - Lịch sử thay đổi trạng thái (`log`)
                    
                    ⚠️ **Lưu ý:**  
                    - BE tự động truyền `Token` từ `application.yml`.  
                    - GHN yêu cầu `order_code` hợp lệ, chỉ trả về đơn hàng thuộc shop của token đó.
                    
                    ---
                    ### 🧱 **Ví dụ request:**
                    ```json
                    {
                      "order_code": "GYNLRKHE"
                    }
                    ```
                    
                    ---
                    ### 📤 **Response mẫu thành công:**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": {
                        "order_code": "GYNLRKHE",
                        "status": "picking",
                        "from_name": "Nguyen",
                        "to_name": "TinTest124",
                        "cod_amount": 200000,
                        "insurance_value": 2000000,
                        "content": "ABCDEF",
                        "leadtime": "2025-11-12T09:00:00Z",
                        "log": [
                          { "status": "picking", "updated_date": "2025-11-10T14:40:00Z" },
                          { "status": "picked", "updated_date": "2025-11-10T14:50:00Z" },
                          { "status": "storing", "updated_date": "2025-11-10T15:00:00Z" }
                        ]
                      }
                    }
                    ```
                    
                    ---
                    ### ⚠️ **Response lỗi (ví dụ):**
                    ```json
                    {
                      "code": 400,
                      "message": "code=401, message=Token is not valid!",
                      "data": null
                    }
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Trả về chi tiết đơn hàng GHN (JSON gốc)",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": {
                                        "order_code": "GYNLRKHE",
                                        "status": "delivering",
                                        "cod_amount": 350000
                                      }
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Sai order_code hoặc Token không hợp lệ")
            }
    )
    @PostMapping("/order-detail")
    public ResponseEntity<String> getOrderDetail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Body JSON chứa mã đơn hàng GHN (`order_code`)",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                                    {
                                      "order_code": "GYNLRKHE"
                                    }
                                    """))
            )
            @RequestBody String body
    ) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(false));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/shipping-order/detail",
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
// 8️⃣ LẤY DANH SÁCH CA LẤY HÀNG (PICK SHIFT)
// ==========================================================
    @Operation(
            summary = "🕒 Lấy danh sách ca lấy hàng GHN (Pick Shift)",
            description = """
                    🧭 **Mục đích:**  
                    Lấy danh sách các **ca lấy hàng (Pick Shift)** mà GHN cung cấp — ví dụ:  
                    - Ca sáng (07h00 – 12h00)  
                    - Ca chiều (12h00 – 18h00)  
                    
                    ⚙️ **Ứng dụng:**  
                    - FE dùng API này để hiển thị danh sách ca lấy hàng cho người bán lựa chọn khi tạo đơn GHN.  
                    - Mỗi `id` tương ứng với 1 khung giờ, có thể gửi trong body `/create-order` như:
                      ```json
                      "pick_shift": [2]
                      ```
                    
                    ⚠️ **Lưu ý:**  
                    - BE **tự động thêm Token từ `application.yml`**, FE không cần truyền token.  
                    - GHN yêu cầu header: `Token: <ghn.token>`.  
                    - Không cần truyền body hay query params.
                    
                    ---
                    ### 📤 **Response mẫu thành công:**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": [
                        {
                          "id": 2,
                          "title": "Ca lấy 12-03-2021 (12h00 - 18h00)",
                          "from_time": 43200,
                          "to_time": 64800
                        },
                        {
                          "id": 3,
                          "title": "Ca lấy 13-03-2021 (7h00 - 12h00)",
                          "from_time": 111600,
                          "to_time": 129600
                        }
                      ]
                    }
                    ```
                    
                    ---
                    ### ⚠️ **Response lỗi (ví dụ):**
                    ```json
                    {
                      "code": 400,
                      "message": "Token is required!",
                      "data": null,
                      "code_message": "USER_ERR_COMMON"
                    }
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - Trả JSON gốc GHN",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": [
                                        { "id": 2, "title": "Ca lấy 12-03-2021 (12h00 - 18h00)" },
                                        { "id": 3, "title": "Ca lấy 13-03-2021 (7h00 - 12h00)" }
                                      ]
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai Token")
            }
    )
    @GetMapping("/pick-shifts")
    public ResponseEntity<String> getPickShifts() {
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(false));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/shift/date",
                HttpMethod.GET,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
// 9️⃣ TẠO TOKEN IN ĐƠN HÀNG GHN (A5 GENERATE TOKEN)
// ==========================================================
    @Operation(
            summary = "🧾 Tạo token in đơn hàng GHN (A5 Generate Token)",
            description = """
                    🧭 **Mục đích:**  
                    Sinh ra 1 token tạm thời (có hiệu lực 30 phút) để **in đơn GHN** theo các kích thước khác nhau:  
                    - A5  
                    - 80x80  
                    - 52x70  
                    
                    ⚙️ **Luồng hoạt động:**
                    1️⃣ FE gọi API này → BE gửi yêu cầu lên GHN.  
                    2️⃣ GHN trả về `token`.  
                    3️⃣ FE hoặc BE có thể **ghép token vào URL** để in đơn.
                    
                    ---
                    ### 🧱 **Ví dụ request:**
                    ```json
                    {
                      "order_codes": ["GYN7Y97T"]
                    }
                    ```
                    
                    ---
                    ### 📤 **Response mẫu:**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": {
                        "token": "e27db030-a1bf-11ea-b421-6a186c15e40e"
                      }
                    }
                    ```
                    
                    ---
                    ### 🖨️ **Cách sử dụng token để in:**
                    > Sau khi lấy token ở bước trên, bạn có thể mở link trực tiếp:
                    
                    | Loại in | URL (Production) | Ví dụ |
                    |----------|------------------|--------|
                    | **A5** | `https://online-gateway.ghn.vn/a5/public-api/printA5?token=ABC` | `https://online-gateway.ghn.vn/a5/public-api/printA5?token=e27db030-a1bf-11ea-b421-6a186c15e40e` |
                    | **80x80** | `https://online-gateway.ghn.vn/a5/public-api/print80x80?token=ABC` |  |
                    | **50x72** | `https://online-gateway.ghn.vn/a5/public-api/print52x70?token=ABC` |  |
                    
                    🕓 Token có hiệu lực trong **30 phút** kể từ khi sinh ra.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - Trả về token in GHN",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": {
                                        "token": "e27db030-a1bf-11ea-b421-6a186c15e40e"
                                      }
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Đơn hàng không tồn tại hoặc Token GHN sai")
            }
    )
    @PostMapping("/print-token")
    public ResponseEntity<String> generatePrintToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Body chứa danh sách mã đơn hàng GHN cần in (`order_codes`)",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                                    {
                                      "order_codes": ["GYN7Y97T"]
                                    }
                                    """))
            )
            @RequestBody String body
    ) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(false));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/a5/gen-token",
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
// 🔟 IN ĐƠN HÀNG GHN (A5 PRINT LABEL)
// ==========================================================
    @Operation(
            summary = "🖨️ In đơn hàng GHN khổ A5 (Print A5 PDF)",
            description = """
                    🧭 **Mục đích:**  
                    Tải hoặc hiển thị nhãn GHN khổ A5 (file PDF) dựa trên token được sinh từ API `/print-token`.
                    
                    ⚙️ **Cách sử dụng:**
                    1️⃣ Gọi `/api/ghn/print-token` để lấy token.  
                    2️⃣ Lấy `data.token` từ response.  
                    3️⃣ Gọi endpoint này với `?token=...` hoặc mở trực tiếp trên trình duyệt.
                    
                    ---
                    ### 🔗 **Ví dụ URL trực tiếp:**
                    ```
                    https://online-gateway.ghn.vn/a5/public-api/printA5?token=e27db030-a1bf-11ea-b421-6a186c15e40e
                    ```
                    
                    ---
                    ### 📤 **Response:**
                    - Trả về **file PDF** chứa nhãn in (base64 hoặc stream).  
                    - Token hết hạn sau **30 phút**.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "File PDF nhãn A5 (trả về dạng stream)"
                    ),
                    @ApiResponse(responseCode = "400", description = "Token sai hoặc hết hạn")
            }
    )
    @GetMapping("/print-a5")
    public ResponseEntity<String> printA5(
            @RequestParam String token
    ) {
        String printUrl = "https://online-gateway.ghn.vn/a5/public-api/printA5?token=" + token;

        ResponseEntity<String> response = restTemplate.exchange(
                printUrl,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders(false)),
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_PDF)
                .body(response.getBody());
    }

    // ==========================================================
// 🔁 CẬP NHẬT GIÁ TRỊ COD CHO ĐƠN GHN
// ==========================================================
    @Operation(
            summary = "💰 Cập nhật COD của đơn hàng GHN",
            description = """
                    🧭 **Mục đích:**  
                    Dùng để cập nhật lại số tiền thu hộ (COD) của 1 đơn hàng GHN sau khi đã tạo đơn.
                    
                    ⚠️ **Lưu ý quan trọng:**  
                    - GHN yêu cầu gửi **Token** trong header.  
                    - Chỉ áp dụng cho đơn GHN hợp lệ và chưa hoàn tất giao hàng.  
                    - `cod_amount` tối đa **5.000.000 VND**.  
                    
                    ---
                    ### 🧱 **Body mẫu**
                    ```json
                    {
                      "order_code": "5E3NK3RS",
                      "cod_amount": 100000
                    }
                    ```
                    
                    ---
                    ### 📤 **Response mẫu**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": null
                    }
                    ```
                    
                    ---
                    ### ❌ **Lỗi thường gặp**
                    ```json
                    {
                      "code": 400,
                      "message": "Đơn hàng không tồn tại hoặc token sai",
                      "data": null
                    }
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - COD đã được cập nhật",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": null
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Lỗi: Token sai hoặc đơn không tồn tại")
            }
    )
    @PostMapping("/update-cod")
    public ResponseEntity<String> updateCodAmount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Body chứa mã đơn và giá trị COD mới",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                                    {
                                      "order_code": "5E3NK3RS",
                                      "cod_amount": 100000
                                    }
                                    """))
            )
            @RequestBody String body
    ) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(false));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/shipping-order/updateCOD",
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // ==========================================================
// 🔁 GỬI LẠI ĐƠN GHN (Delivery Again)
// ==========================================================
    @Operation(
            summary = "🔄 Gửi lại đơn GHN (Delivery Again)",
            description = """
                    🧭 **Mục đích:**  
                    Cho phép shop yêu cầu GHN **giao lại đơn hàng** sau khi giao thất bại.
                    
                    ⚠️ **Lưu ý:**  
                    - Chỉ áp dụng cho đơn có trạng thái chờ giao lại (`waiting for delivery`).  
                    - Sau khi gọi API thành công, trạng thái đơn chuyển thành `"storage"`.  
                    - Cần truyền **Token** và **ShopId** trong header.  
                    - Mặc định GHN chỉ cho phép giao lại trong vòng **24h** sau khi đơn thất bại.
                    
                    ---
                    ### 🧱 **Body mẫu**
                    ```json
                    {
                      "order_codes": ["5ENLKKHD"]
                    }
                    ```
                    
                    ---
                    ### 📤 **Response mẫu**
                    ```json
                    {
                      "code": 200,
                      "message": "Success",
                      "data": [
                        {
                          "order_code": "5ENLKKHD",
                          "result": true,
                          "message": "OK"
                        }
                      ]
                    }
                    ```
                    
                    ---
                    ### ❌ **Lỗi thường gặp**
                    ```json
                    {
                      "code": 400,
                      "message": "ShopID is invalid: SHOP_NOT_FOUND",
                      "data": null
                    }
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công - Đơn đã được gửi lại GHN",
                            content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    {
                                      "code": 200,
                                      "message": "Success",
                                      "data": [
                                        {
                                          "order_code": "5ENLKKHD",
                                          "result": true,
                                          "message": "OK"
                                        }
                                      ]
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "Lỗi: ShopID hoặc Token không hợp lệ")
            }
    )
    @PostMapping("/delivery-again")
    public ResponseEntity<String> deliveryAgain(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Danh sách mã đơn cần gửi lại GHN",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                                    {
                                      "order_codes": ["5ENLKKHD"]
                                    }
                                    """))
            )
            @RequestBody String body
    ) {
        HttpEntity<String> entity = new HttpEntity<>(body, createHeaders(true));

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/v2/switch-status/storing",
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }


}
