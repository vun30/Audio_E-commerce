//package org.example.audio_ecommerce.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.*;
//import io.swagger.v3.oas.annotations.responses.*;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.request.AssignDeliveryRequest;
//import org.example.audio_ecommerce.dto.request.ConfirmSuccessRequest;
//import org.example.audio_ecommerce.dto.request.DenyReceiveRequest;
//import org.example.audio_ecommerce.dto.request.PushLocationRequest;
//import org.example.audio_ecommerce.dto.response.BaseResponse;
//import org.example.audio_ecommerce.dto.response.DeliveryAssignmentResponse;
//import org.example.audio_ecommerce.entity.DeliveryAssignment;
//import org.example.audio_ecommerce.entity.Enum.OrderStatus;
//import org.example.audio_ecommerce.entity.StoreOrder;
//import org.example.audio_ecommerce.service.DeliveryService;
//import org.springframework.data.domain.Page;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.UUID;
//
//@Tag(
//        name = "Store Delivery",
//        description = """
//                Bộ API cho quy trình giao hàng nội bộ của cửa hàng:
//                • Phân công nhân viên → chuẩn bị hàng (READY_FOR_PICKUP)
//                • Shipper nhận hàng và bắt đầu giao (OUT_FOR_DELIVERY)
//                • Đến nơi chờ xác nhận (DELIVERED_WAITING_CONFIRM)
//                • Xác nhận giao thành công (DELIVERY_SUCCESS) hoặc từ chối nhận (DELIVERY_DENIED)
//                • Shipper đẩy vị trí định kỳ mỗi 4–5 phút để theo dõi lộ trình.
//                """
//)
//@RestController
//@RequestMapping("/api/v1/stores/{storeId}/orders/{storeOrderId}/delivery")
//@RequiredArgsConstructor
//public class StoreDeliveryController {
//
//    private final DeliveryService deliveryService;
//
//    // ==============================
//    // 👤 PHÂN CÔNG NHÂN VIÊN GIAO HÀNG
//    // ==============================
//    @Operation(
//            summary = "Phân công nhân viên giao hàng và (tuỳ chọn) nhân viên chuẩn bị",
//            description = """
//                    - Dùng cho quản lý/kho để gán **deliveryStaff** chịu trách nhiệm giao đơn.
//                    - Có thể chỉ định **preparedByStaff** (nhân viên kho) để ghi nhận ai chuẩn bị hàng.
//                    - Sau khi phân công, đơn chuyển trạng thái **READY_FOR_PICKUP**.
//
//                    Yêu cầu quyền: **Staff thuộc đúng Store** (manager/admin).
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Phân công thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder/Staff"),
//            @ApiResponse(responseCode = "403", description = "Staff không thuộc store này hoặc không đủ quyền")
//    })
//    @PostMapping("/assign")
//    public BaseResponse<StoreOrder> assign(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true, example = "9b4b4e0f-6f1f-4a5c-8e5f-2f21b4ac7f10")
//            @PathVariable UUID storeId,
//
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true, example = "3b50b1a9-5a97-4f7f-8a9f-8f1b3d1c2a77")
//            @PathVariable UUID storeOrderId,
//
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = """
//                            - `deliveryStaffId` (bắt buộc): nhân viên giao hàng
//                            - `preparedByStaffId` (tuỳ chọn): nhân viên kho chuẩn bị
//                            - `note`: ghi chú giao hàng (ví dụ: giao giờ HC)
//                            """,
//                    required = true,
//                    content = @Content(schema = @Schema(implementation = AssignDeliveryRequest.class))
//            )
//            @RequestBody AssignDeliveryRequest req
//    ) {
//        deliveryService.assignDeliveryStaff(storeId, storeOrderId, req.getDeliveryStaffId(), req.getPreparedByStaffId(), req.getNote());
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("✅ Phân công nhân viên giao hàng thành công", order);
//    }
//
//    // ==============================
//    // 📦 XÁC NHẬN CHUẨN BỊ XONG (READY_FOR_PICKUP)
//    // ==============================
//    @Operation(
//            summary = "Kho xác nhận đã chuẩn bị xong (READY_FOR_PICKUP)",
//            description = """
//                    - Gọi khi hàng đã được đóng gói xong, sẵn sàng để shipper nhận.
//                    - Đơn chuyển/trụ tại trạng thái **READY_FOR_PICKUP**.
//                    - Có thể bỏ qua nếu đã gọi /assign (vì /assign cũng set READY_FOR_PICKUP).
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder")
//    })
//    @PostMapping("/ready")
//    public BaseResponse<StoreOrder> readyForPickup(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId
//    ) {
//        deliveryService.markReadyForPickup(storeId, storeOrderId);
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("📦 Đã đánh dấu READY_FOR_PICKUP", order);
//    }
//
//    // ==============================
//    // 🚚 BẮT ĐẦU GIAO (OUT_FOR_DELIVERY)
//    // ==============================
//    @Operation(
//            summary = "Shipper nhận hàng và bắt đầu giao (OUT_FOR_DELIVERY)",
//            description = """
//                    - Shipper bấm khi rời kho/ cửa hàng để bắt đầu lộ trình.
//                    - Từ thời điểm này, app shipper nên bật **Foreground Service** và **đẩy vị trí định kỳ** qua `/location`.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder hoặc chưa có assignment")
//    })
//    @PostMapping("/out-for-delivery")
//    public BaseResponse<StoreOrder> outForDelivery(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId
//    ) {
//        deliveryService.markOutForDelivery(storeId, storeOrderId);
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("🚚 Đã đánh dấu OUT_FOR_DELIVERY", order);
//    }
//
//    // ==============================
//    // 📍 ĐẾN NƠI, CHỜ XÁC NHẬN (DELIVERED_WAITING_CONFIRM)
//    // ==============================
//    @Operation(
//            summary = "Shipper đánh dấu đã đến địa chỉ khách (DELIVERED_WAITING_CONFIRM)",
//            description = """
//                    - Gọi khi shipper đã tới điểm giao.
//                    - Đơn chuyển trạng thái **DELIVERED_WAITING_CONFIRM** để chờ khách/biên bản/ảnh.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder hoặc chưa có assignment")
//    })
//    @PostMapping("/arrived")
//    public BaseResponse<StoreOrder> deliveredWaitingConfirm(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId
//    ) {
//        deliveryService.markDeliveredWaitingConfirm(storeId, storeOrderId);
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("📍 Đã đến nơi, chờ xác nhận (DELIVERED_WAITING_CONFIRM)", order);
//    }
//
//    // ==============================
//    // ✅ GIAO THÀNH CÔNG (DELIVERY_SUCCESS)
//    // ==============================
//    @Operation(
//            summary = "Xác nhận giao hàng thành công (kèm ảnh/lắp đặt)",
//            description = """
//                    - Shipper tải **ảnh biên bản**/ảnh giao hàng lên (đã up sẵn & gửi `photoUrl`).
//                    - `installed=true` nếu đã lắp đặt tại chỗ.
//                    - Đơn chuyển **DELIVERY_SUCCESS**.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Xác nhận thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder"),
//            @ApiResponse(responseCode = "400", description = "Thiếu tham số bắt buộc (ví dụ photoUrl)")
//    })
//    @PostMapping("/success")
//    public BaseResponse<StoreOrder> confirmSuccess(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId,
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = """
//                            - `photoUrl` (khuyến nghị): URL ảnh chứng minh đã giao/lắp đặt.
//                            - `installed`: đã lắp đặt tại chỗ hay chưa.
//                            - `note`: ghi chú thêm (ví dụ: đã hướng dẫn sử dụng).
//                            """,
//                    required = true,
//                    content = @Content(schema = @Schema(implementation = ConfirmSuccessRequest.class))
//            )
//            @RequestBody ConfirmSuccessRequest req
//    ) {
//        deliveryService.confirmDeliverySuccess(storeId, storeOrderId, req.getPhotoUrl(),
//                Boolean.TRUE.equals(req.getInstalled()), req.getNote());
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("✅ Giao thành công (DELIVERY_SUCCESS)", order);
//    }
//
//    // ==============================
//    // ❌ KHÁCH TỪ CHỐI NHẬN (DELIVERY_DENIED)
//    // ==============================
//    @Operation(
//            summary = "Đánh dấu khách không nhận hàng (deny receive)",
//            description = """
//                    - Shipper nhập lý do khách không nhận (ví dụ: đi vắng, đổi ý...).
//                    - Đơn chuyển **DELIVERY_DENIED** và ghi chú lý do vào `shipNote`.
//                    - Tùy chính sách: có thể mở flow hoàn hàng/hoàn tiền riêng (không nằm trong API này).
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder")
//    })
//    @PostMapping("/deny")
//    public BaseResponse<StoreOrder> denyReceive(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId,
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = """
//                            - `reason`: lý do khách từ chối nhận.
//                            """,
//                    required = true,
//                    content = @Content(schema = @Schema(implementation = DenyReceiveRequest.class))
//            )
//            @RequestBody DenyReceiveRequest req
//    ) {
//        deliveryService.markDeliveryDenied(storeId, storeOrderId, req.getReason());
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("❌ Khách từ chối nhận (DELIVERY_DENIED)", order);
//    }
//
//    // ==============================
//    // 🗺️ ĐẨY VỊ TRÍ ĐỊNH KỲ (4–5 PHÚT)
//    // ==============================
//    @Operation(
//            summary = "Shipper đẩy vị trí hiện tại (định kỳ 4–5 phút khi đang giao)",
//            description = """
//                    - App shipper chạy **Foreground Service** (Android) để gửi vị trí sau mỗi 4–5 phút.
//                    - Hệ thống ghi `latitude/longitude/speedKmh/addressText` vào nhật ký định tuyến.
//                    - Nếu đơn còn ở trạng thái READY_FOR_PICKUP thì hệ thống tự chuyển sang OUT_FOR_DELIVERY sau lần đẩy đầu tiên.
//
//                    Gợi ý: khi `status=OUT_FOR_DELIVERY`, hiển thị polyline route theo các điểm log để theo dõi.
//                    """
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Ghi nhận vị trí thành công",
//                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy StoreOrder hoặc chưa có assignment"),
//            @ApiResponse(responseCode = "400", description = "Toạ độ không hợp lệ")
//    })
//    @PostMapping("/location")
//    public BaseResponse<StoreOrder> pushLocation(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID đơn của cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeOrderId,
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = """
//                            - `latitude`/`longitude`: toạ độ WGS84.
//                            - `speedKmh` (tuỳ chọn): tốc độ tính bằng km/h.
//                            - `addressText` (tuỳ chọn): địa chỉ text nếu client đã reverse-geocode.
//                            """,
//                    required = true,
//                    content = @Content(schema = @Schema(implementation = PushLocationRequest.class))
//            )
//            @RequestBody PushLocationRequest req
//    ) {
//        deliveryService.pushLocation(storeId, storeOrderId, req.getLatitude(), req.getLongitude(),
//                req.getSpeedKmh(), req.getAddressText());
//        StoreOrder order = deliveryService.getStoreOrderEntity(storeOrderId);
//        return BaseResponse.success("🗺️ Đã ghi nhận vị trí", order);
//    }
//
//    // ==============================
//// 📋 LIST ASSIGNMENTS (ALL/STATUS)
//// ==============================
//    @Operation(
//            summary = "Danh sách phân công giao hàng của cửa hàng",
//            description = """
//                    - Trả về **tất cả phân công** (DeliveryAssignment) thuộc cửa hàng.
//                    - Có thể lọc theo `status` của **StoreOrder** (READY_FOR_PICKUP / OUT_FOR_DELIVERY / ...).
//                    - Phục vụ màn hình quản trị theo dõi tiến độ.
//                    """)
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công",
//                    content = @Content(mediaType = "application/json",
//                            schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy Store")
//    })
//    @GetMapping("/assignments")
//    public BaseResponse<?> listAssignments(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "Lọc theo trạng thái StoreOrder", schema = @Schema(implementation = OrderStatus.class))
//            @RequestParam(required = false) OrderStatus status
//    ) {
//        var data = deliveryService.listAssignments(storeId, status);
//        return BaseResponse.success("📋 Danh sách phân công", data);
//    }
//
//    // ==============================
//// 📄 PAGE ASSIGNMENTS (ALL/STATUS)
//// ==============================
//    @Operation(
//            summary = "Phân trang phân công giao hàng của cửa hàng",
//            description = """
//                    - Giống `/assignments` nhưng **có phân trang & sắp xếp**.
//                    - `sort` mặc định theo `assignedAt desc`.
//                    - Dùng cho bảng dữ liệu lớn.
//                    """)
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Phân trang thành công",
//                    content = @Content(mediaType = "application/json",
//                            schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy Store")
//    })
//    @GetMapping("/assignments/page")
//    public BaseResponse<?> pageAssignments(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "Lọc theo trạng thái StoreOrder", schema = @Schema(implementation = OrderStatus.class))
//            @RequestParam(required = false) OrderStatus status,
//            @Parameter(description = "Trang (0-based)", example = "0")
//            @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "Kích thước trang", example = "20")
//            @RequestParam(defaultValue = "20") int size,
//            @Parameter(description = "Trường sort (vd: assignedAt, deliveredAt...)", example = "assignedAt")
//            @RequestParam(required = false) String sort
//    ) {
//        var data = deliveryService.pageAssignments(storeId, status, page, size, sort);
//        return BaseResponse.success("📄 Phân trang phân công", data);
//    }
//
//    // ==============================
//// ℹ️ GET ONE ASSIGNMENT
//// ==============================
//    @Operation(
//            summary = "Chi tiết một phân công giao hàng",
//            description = """
//                    - Trả về thông tin chi tiết của **DeliveryAssignment** theo `assignmentId`.
//                    - Bảo vệ: chỉ truy cập được nếu thuộc đúng `storeId`.
//                    """)
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công",
//                    content = @Content(mediaType = "application/json",
//                            schema = @Schema(implementation = BaseResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy Assignment hoặc không thuộc Store"),
//            @ApiResponse(responseCode = "403", description = "Không có quyền xem")
//    })
//    @GetMapping("/assignments/{assignmentId}")
//    public BaseResponse<?> getAssignment(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID phân công (UUID)", required = true)
//            @PathVariable UUID assignmentId
//    ) {
//        var data = deliveryService.getAssignment(storeId, assignmentId);
//        return BaseResponse.success("ℹ️ Chi tiết phân công", data);
//    }
//
//    // ==============================
//// 👤 LIST ASSIGNMENTS OF STAFF
//// ==============================
//    @Operation(
//            summary = "Danh sách phân công theo nhân viên giao hàng",
//            description = """
//                    - Trả về tất cả phân công thuộc **deliveryStaff** chỉ định và đúng `storeId`.
//                    - Có thể lọc theo `status` của StoreOrder để xem việc đang làm của nhân viên.
//                    """)
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công",
//                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeliveryAssignmentResponse.class)))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy Store/Staff"),
//            @ApiResponse(responseCode = "403", description = "Staff không thuộc Store")
//    })
//    @GetMapping("/staff/{staffId}/assignments")
//    public ResponseEntity<List<DeliveryAssignmentResponse>> listOfStaff(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID nhân viên giao hàng (UUID)", required = true)
//            @PathVariable UUID staffId,
//            @Parameter(description = "Lọc theo trạng thái StoreOrder", schema = @Schema(implementation = OrderStatus.class))
//            @RequestParam(required = false) OrderStatus status
//    ) {
//        var data = deliveryService.listAssignmentsOfStaff(storeId, staffId, status);
//        return ResponseEntity.ok(data);
//    }
//
//    // ==============================
//// 👤 PAGE ASSIGNMENTS OF STAFF
//// ==============================
//    @Operation(
//            summary = "Phân trang phân công theo nhân viên giao hàng",
//            description = """
//                    - Tương tự `/staff/{staffId}/assignments` nhưng **có phân trang/sort**.
//                    - Dùng cho màn hình theo dõi workload của từng shipper.
//                    """)
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Phân trang thành công",
//                    content = @Content(schema = @Schema(implementation = Page.class))),
//            @ApiResponse(responseCode = "404", description = "Không tìm thấy Store/Staff"),
//            @ApiResponse(responseCode = "403", description = "Staff không thuộc Store")
//    })
//    @GetMapping("/staff/{staffId}/assignments/page")
//    public ResponseEntity<Page<DeliveryAssignmentResponse>> pageOfStaff(
//            @Parameter(description = "ID cửa hàng (UUID)", required = true)
//            @PathVariable UUID storeId,
//            @Parameter(description = "ID nhân viên giao hàng (UUID)", required = true)
//            @PathVariable UUID staffId,
//            @Parameter(description = "Lọc theo trạng thái StoreOrder", schema = @Schema(implementation = OrderStatus.class))
//            @RequestParam(required = false) OrderStatus status,
//            @Parameter(description = "Trang (0-based)", example = "0")
//            @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "Kích thước trang", example = "20")
//            @RequestParam(defaultValue = "20") int size,
//            @Parameter(description = "Trường sort (vd: assignedAt, deliveredAt...)", example = "assignedAt")
//            @RequestParam(required = false) String sort
//    ) {
//        var data = deliveryService.pageAssignmentsOfStaff(storeId, staffId, status, page, size, sort);
//        return ResponseEntity.ok(data);
//    }
//
//}
