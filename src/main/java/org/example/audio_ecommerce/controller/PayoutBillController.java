package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.PayoutBillResponse;
import org.example.audio_ecommerce.dto.response.PayoutOverviewResponse;
import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;
import org.example.audio_ecommerce.entity.PayoutBill;
import org.example.audio_ecommerce.service.PayoutBillService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payout-bill")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payout Bill (Admin)", description = "API quản lý tạo bill — thống kê doanh thu, phí ship giao hàng + phí ship hoàn hàng cho shop")
public class PayoutBillController {

    private final PayoutBillService payoutBillService;


    // =====================================================================
    // 1. Admin tạo bill cho 1 shop
    // =====================================================================
    @Operation(
            summary = "Tạo payout bill cho shop",
            description = """
                    Admin tạo bill đối soát cho 1 shop theo các rule:
                    - Lấy tất cả OrderItem:
                        • eligibleForPayout = true
                        • isPayout = false
                    - Lấy phí ship GHN ở StoreOrder:
                        • paidByShop = false
                    - Lấy phí ship ReturnShippingFee:
                        • paidByShop = false
                            
                    Sau khi tạo bill:
                    - FE nhận bill gồm:
                        • danh sách item payout
                        • danh sách phí ship GHN
                        • danh sách phí return
                        • tổng gross / totalFee / totalNet
                    """
    )
    @PostMapping("/create/{storeId}")
    public ResponseEntity<PayoutBillResponse> createBill(
            @Parameter(description = "ID shop cần tạo bill payout")
            @PathVariable UUID storeId) {

        PayoutBill created = payoutBillService.createBillForShop(storeId);
        return ResponseEntity.ok(PayoutBillResponse.fromEntity(created));
    }


    // =====================================================================
    // 2. Admin lấy bill PENDING của shop (tự động tạo nếu chưa có)
    // =====================================================================
    @Operation(
            summary = "Lấy bill gần nhất của shop (Tự tạo nếu chưa có)",
            description = """
                    FE gọi API này để lấy bill payout hiện tại của shop.
                    • Nếu shop đã có bill PENDING → trả về bill đó
                    • Nếu chưa có bill → hệ thống kiểm tra dữ liệu đủ điều kiện và tự tạo bill mới
                    """
    )
    @GetMapping("/current/{storeId}")
    public ResponseEntity<PayoutBillResponse> getCurrentBill(
            @Parameter(description = "ID shop cần xem bill payout")
            @PathVariable UUID storeId) {

        PayoutBill bill = payoutBillService.getOrCreateBillForStore(storeId);
        return ResponseEntity.ok(PayoutBillResponse.fromEntity(bill));
    }


    // =====================================================================
    // 3. Admin xem chi tiết bill theo ID
    // =====================================================================
    @Operation(
            summary = "Xem chi tiết payout bill theo ID",
            description = """
                    Trả về chi tiết đầy đủ:
                    • danh sách item payout  
                    • danh sách phí ship giao hàng  
                    • danh sách phí ship hoàn hàng  
                    • tổng tiền hàng  
                    • tổng phí nền tảng  
                    • tổng phí ship  
                    • tổng net payout  
                    """
    )
    @GetMapping("/{billId}")
    public ResponseEntity<PayoutBillResponse> getBillById(
            @Parameter(description = "ID của bill payout")
            @PathVariable UUID billId) {

        PayoutBill bill = payoutBillService.getFullBill(billId);
        return ResponseEntity.ok(PayoutBillResponse.fromEntity(bill));
    }


    // =====================================================================
    // 4. Admin xác nhận đã thanh toán (PAY BILL)
    // =====================================================================
    @Operation(
            summary = "Admin xác nhận đã thanh toán bill cho shop",
            description = """
                    Sau khi admin chuyển khoản cho shop, call API này để cập nhật:
                    • bill.status → PAID
                    • Lưu mã giao dịch (reference code)
                    • Lưu ảnh chứng từ thanh toán (receipt URL)
                    • Update:
                        - OrderItem.isPayout = true
                        - StoreOrder.paidByShop = true (phí GHN)
                        - ReturnShippingFee.paidByShop = true (phí return)
                    """
    )
    @PostMapping("/{billId}/mark-paid")
    public ResponseEntity<PayoutBillResponse> markBillAsPaid(
            @Parameter(description = "ID bill cần xác nhận đã thanh toán")
            @PathVariable UUID billId,

            @Parameter(description = "Mã giao dịch chuyển khoản")
            @RequestParam(required = false) String reference,

            @Parameter(description = "URL ảnh chứng từ chuyển khoản")
            @RequestParam(required = false) String proofImageUrl,

            @Parameter(description = "Ghi chú của admin")
            @RequestParam(required = false) String note
    ) {
        PayoutBill bill = payoutBillService.markBillAsPaid(billId, reference, proofImageUrl, note);
        return ResponseEntity.ok(PayoutBillResponse.fromEntity(bill));
    }


    // =====================================================================
    // 5. Admin tạo bill tự động cho toàn bộ shop (cron/manual)
    // =====================================================================
    @Operation(
            summary = "Tự tạo bill payout cho TẤT CẢ SHOP",
            description = """
                    • Chạy theo CRON hoặc admin trigger
                    • Chỉ tạo bill nếu shop có dữ liệu payout:
                        - OrderItem eligibleForPayout = true
                        - ShippingFee paidByShop = false
                        - ReturnShippingFee paidByShop = false
                    • Trả về danh sách bill đã tạo
                    """
    )
    @PostMapping("/auto-create")
    public ResponseEntity<List<PayoutBillResponse>> autoCreateBills() {

        List<PayoutBill> bills = payoutBillService.autoCreateBillsForAllStores();

        return ResponseEntity.ok(
                bills.stream().map(PayoutBillResponse::fromEntity).toList()
        );
    }

    @Operation(
        summary = "Lấy danh sách payout bill (kèm bộ lọc)",
        description = """
                API dùng cho FE để hiển thị danh sách bill payout của shop hoặc toàn hệ thống.

                🎯 Bộ lọc hỗ trợ:
                • storeId (UUID, optional)
                    - Nếu truyền → trả về bill của shop đó
                    - Nếu bỏ trống → admin lấy toàn bộ bill hệ thống

                • status (PayoutBillStatus, optional)
                    - PENDING / PAID / CANCELED / ... 
                    - Cho phép FE lọc theo trạng thái bill

                • fromDate, toDate (LocalDateTime ISO, optional)
                    - Lọc theo khoảng ngày tạo bill
                    - Format: yyyy-MM-dd'T'HH:mm:ss
                    - Nếu bỏ trống → không giới hạn

                • billCode (String, optional)
                    - Search theo mã bill (PB-xxxx)
                    - Hỗ trợ tìm gần đúng (LIKE)

                🎁 Kết quả trả về:
                • Danh sách PayoutBill đã apply đầy đủ filter
                • Sắp xếp theo createdAt DESC (bill mới nhất nằm đầu)

                ⚠️ Gợi ý FE:
                • Luôn truyền fromDate/toDate khi làm màn lịch sử
                • Để lấy bill shop hiện tại → chỉ cần truyền storeId
                """
)
@GetMapping
public ResponseEntity<List<PayoutBill>> getBills(
        @RequestParam(required = false) UUID storeId,
        @RequestParam(required = false) PayoutBillStatus status,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @RequestParam(required = false) String billCode
) {

    List<PayoutBill> list = payoutBillService.listBills(
            storeId,
            status,
            fromDate,
            toDate,
            billCode
    );

    return ResponseEntity.ok(list);
}

  @GetMapping("/overview/{storeId}")
    public ResponseEntity<PayoutOverviewResponse> getOverview(
            @PathVariable UUID storeId,

            @RequestParam("from")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime fromDate,

            @RequestParam("to")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime toDate
    ) {

        PayoutOverviewResponse response =
                payoutBillService.getOverview(storeId, fromDate, toDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/overview")
public ResponseEntity<PayoutOverviewResponse> getOverviewAllStores(

        @RequestParam("from")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime fromDate,

        @RequestParam("to")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime toDate
) {

    PayoutOverviewResponse response =
            payoutBillService.getOverview(null, fromDate, toDate); // ⬅ null = ALL STORES

    return ResponseEntity.ok(response);
}
}
