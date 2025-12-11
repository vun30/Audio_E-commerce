package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.SettlementReportResponse;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.SettlementReportType;
import org.example.audio_ecommerce.service.Impl.SettlementReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Tag(
        name = "📊 Settlement / Payout Reports",
        description = """
        API cho báo cáo settlement/payout — FE dùng để hiện dashboard tài chính, báo cáo payout, platform fee, v.v.
        - Endpoint chính: GET /api/v1/settlement/reports
        - Trả về: SettlementReportResponse (entries + tổng)
        """
)
@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
@Slf4j
public class SettlementReportController {

    private final SettlementReportService reportService;

    @Operation(
            summary = "📈 Lấy báo cáo settlement/payout",
            description = """
            Query params:
            - type (required): UNDELI_COD | UNDELI_ONLINE | DELI_COD | DELI_ONLINE | PLATFORM_FEE_TO_COLLECT | TOTAL_COLLECTED
            - date (optional, ISO date yyyy-MM-dd): required for DELI_* and PLATFORM_FEE_TO_COLLECT / TOTAL_COLLECTED
            - storeId (optional): filter theo cửa hàng
            - page,size (optional): reserved for pagination in future (service currently returns full list)
            
            Example:
            GET /api/v1/settlement/reports?type=Deli_online&date=2025-12-11
            """
    )
    @GetMapping("/reports")
    public ResponseEntity<BaseResponse> getSettlementReport(
            @Parameter(description = "Loại báo cáo", required = true)
            @RequestParam("type") SettlementReportType type,

            @Parameter(description = "Ngày liên quan (ISO yyyy-MM-dd). Bắt buộc cho DELI_* và PLATFORM_FEE_TO_COLLECT / TOTAL_COLLECTED", required = false)
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Lọc theo storeId (optional)")
            @RequestParam(value = "storeId", required = false) UUID storeId,

            @Parameter(description = "Trang (reserved, optional)")
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "Số item trên trang (reserved, optional)")
            @RequestParam(value = "size", required = false, defaultValue = "100") @Min(1) Integer size
    ) {
        log.info("REST request to get settlement report: type={}, date={}, storeId={}, page={}, size={}",
                type, date, storeId, page, size);

        // Service currently ignores page/size (returns full). Controller keeps them for future.
        SettlementReportResponse resp = reportService.getReport(type, date, storeId);

        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy báo cáo settlement thành công")
                .data(resp)
                .build());
    }

    // Optional: convenience endpoint to fetch only totals (summary) — FE may call when it needs only the totalAmount
    @Operation(summary = "📌 Lấy tổng theo báo cáo (chỉ trả totalAmount)", description = "Trả về chỉ field totalAmount trong SettlementReportResponse")
    @GetMapping("/reports/summary")
    public ResponseEntity<BaseResponse> getSettlementReportSummary(
            @RequestParam("type") SettlementReportType type,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "storeId", required = false) UUID storeId
    ) {
        log.info("REST request to get settlement report summary: type={}, date={}, storeId={}", type, date, storeId);
        SettlementReportResponse resp = reportService.getReport(type, date, storeId);

        // Build a small payload containing only the totalAmount (and report metadata)
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reportType", resp == null ? null : resp.getReportType());
        summary.put("date", resp == null ? null : resp.getDate());
        summary.put("storeId", storeId);
        summary.put("totalAmount", resp == null ? null : resp.getTotalAmount());

        return ResponseEntity.ok(BaseResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy summary báo cáo settlement thành công")
                .data(summary)
                .build());
    }
}
