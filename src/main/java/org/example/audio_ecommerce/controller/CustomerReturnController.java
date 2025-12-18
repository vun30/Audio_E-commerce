package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerReturnComplaintCreateRequest;
import org.example.audio_ecommerce.dto.request.ReturnPackageInfoRequest;
import org.example.audio_ecommerce.dto.request.ReturnRequestCreateRequest;
import org.example.audio_ecommerce.dto.response.ReturnPackageFeeResponse;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.service.CustomerReturnComplaintService;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers/me/returns")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final ReturnRequestService returnService;
    private final CustomerReturnComplaintService complaintService;

    @Operation(
            summary = "Customer tạo yêu cầu hoàn trả/hoàn tiền cho 1 sản phẩm trong đơn",
            description = """
                ✅ FE dùng khi: khách bấm "Yêu cầu hoàn" ở chi tiết đơn hàng.
                
                Input:
                - orderItemId: ID của CustomerOrderItem muốn hoàn
                - productId: để verify khớp với orderItem (anti fake)
                - reasonType: CUSTOMER_FAULT / SHOP_FAULT
                - reason + media (ảnh/video) phía customer
                
                🔒 QUAN TRỌNG:
                - FE KHÔNG gửi giá tiền. Số tiền hoàn (itemPrice) được BE tự tính từ snapshot của order item.
                - BE sẽ validate orderItem thuộc về customer hiện tại (JWT).
                
                Flow sau khi tạo:
                1) ReturnRequest tạo status = PENDING
                2) Order có thể được chuyển trạng thái RETURN_REQUESTED (tuỳ rule)
                3) Shop vào /api/store/returns để approve → tạo GHN → nhận hàng → refund/ dispute
                
                Example request:
                {
                  "orderItemId": "UUID",
                  "productId": "UUID",
                  "reasonType": "SHOP_FAULT",
                  "reason": "Sản phẩm lỗi",
                  "customerVideoUrl": "https://...",
                  "customerImageUrls": ["https://...","https://..."]
                }
                
                Example response:
                {
                  "status": "PENDING",
                  "itemPrice": 60000,
                  "shippingFee": 0,
                  "customerId": "...",
                  "shopId": "...",
                  "orderItemId": "...",
                  "ghnOrderCode": null
                }
                """
    )
    @PostMapping
    public ReturnRequestResponse create(@Valid @RequestBody ReturnRequestCreateRequest req) {
        return returnService.createReturnRequest(req);
    }

    @Operation(
            summary = "Customer xem danh sách yêu cầu hoàn",
            description = """
                ✅ FE dùng khi: màn "Đơn hoàn/Trả hàng" của khách.
                
                Pagination:
                - page: default 0
                - size: default 20
                
                Response: danh sách ReturnRequestResponse (status, itemPrice, shippingFee, tracking...)
                """
    )
    @GetMapping
    public Page<ReturnRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listForCurrentCustomer(pageable);
    }

    @Operation(
            summary = "Customer nhập thông tin kiện hàng để tính phí vận chuyển hoàn",
            description = """
                ✅ FE dùng khi: sau khi shop approve return và yêu cầu customer đóng gói gửi lại.
                
                Input: packageWeight/length/width/height + địa chỉ pickup của customer (tuỳ DTO)
                
                Output:
                - ReturnPackageFeeResponse: phí ship return, bên nào chịu phí (tuỳ rule), và lưu vào ReturnRequest.shippingFee
                
                Lưu ý:
                - API này chỉ set package info + tính phí, KHÔNG tạo GHN order.
                - Tạo GHN order là bên Shop gọi: /api/store/returns/{id}/create-ghn-order
                """
    )
    @PostMapping("/{id}/package-info")
    public ReturnPackageFeeResponse setPackageInfo(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnPackageInfoRequest req
    ) {
        return returnService.setPackageInfoAndCalculateFee(id, req);
    }

    @Operation(
            summary = "Customer tạo khiếu nại/complaint liên quan return",
            description = """
                ✅ FE dùng khi: customer không đồng ý với xử lý của shop (reject/refund amount/...)
                
                Action:
                - Tạo complaint record (phục vụ SLA + auto-refund nếu quá hạn)
                
                Response: 204 NO_CONTENT nếu tạo thành công.
                """
    )
    @PostMapping("/complaints")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createComplaint(@Valid @RequestBody CustomerReturnComplaintCreateRequest req) {
        complaintService.createComplaint(req);
    }
}
