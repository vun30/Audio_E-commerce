package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.ReturnPreviewResponse;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerOrderController {

    private final ReturnRequestService returnService;

    @Operation(
            summary = "Preview số tiền hoàn theo từng sản phẩm trong đơn",
            description = """
                    ✅ FE dùng khi: mở màn hình "Yêu cầu hoàn" trong chi tiết đơn hàng.
                    
                    Mục đích:
                    - Trả về danh sách item trong order + số tiền có thể hoàn (refund preview)
                    - FE dùng để hiển thị rõ: hoàn item nào, hoàn bao nhiêu tiền (chưa tính ship)
                    
                    🔒 Rule quan trọng:
                    - Refund amount KHÔNG do FE tự tính.
                    - Refund amount lấy từ snapshot đã lưu trong CustomerOrderItem (amountCharged / finalLineTotal).
                    - KHÔNG bao gồm phí ship (ship return xử lý riêng trong flow return).
                    
                    FE gắn flow:
                    1) Gọi GET /api/orders/{orderId}/return-preview để hiển thị list item + refundableAmount
                    2) User chọn item muốn hoàn → FE gọi:
                       POST /api/customers/me/returns
                       body gồm orderItemId + reason + media
                    3) Sau đó theo dõi status return ở:
                       GET /api/customers/me/returns
                    
                    Example response:
                    {
                      "orderId": "51bd434f-f885-4478-a0c7-60da812cf013",
                      "items": [
                        {
                          "orderItemId": "173e9890-014a-4ffb-aefd-1159d5ff126e",
                          "productId": "9846f658-216d-497c-915d-25bf5e813a8c",
                          "productName": "Sony Chính hãng New 2025",
                          "quantity": 1,
                          "refundableAmount": 60000,
                          "finalLineTotal": 60000,
                          "amountCharged": 60000
                        }
                      ]
                    }
                    
                    Error cases:
                    - 404: order không tồn tại hoặc không thuộc customer hiện tại
                    - 403: cố truy cập order của người khác
                    """
    )
    @ApiResponse(responseCode = "200", description = "Preview refund amount theo từng item trong order")
    @GetMapping("/api/orders/{orderId}/return-preview")
    public ReturnPreviewResponse returnPreview(@PathVariable UUID orderId) {
        return returnService.previewReturnForOrder(orderId);
    }
}

