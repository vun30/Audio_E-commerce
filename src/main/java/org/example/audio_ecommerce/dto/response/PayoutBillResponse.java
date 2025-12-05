package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.PayoutBill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PayoutBillResponse {

    private UUID id;
    private UUID shopId;
    private String billCode;
    private LocalDateTime createdAt;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    private BigDecimal totalGross;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalShippingOrderFee;
    private BigDecimal totalReturnShippingFee;
    private BigDecimal totalNetPayout;

    private String status;

    private String transferReference;
    private String receiptImageUrl;
    private String adminNote;

    private List<PayoutBillItemResponse> items;
    private List<PayoutShippingOrderFeeResponse> shippingOrders;
    private List<PayoutReturnShippingFeeResponse> returnFees;

    // ================================
    // FROM ENTITY
    // ================================
    public static PayoutBillResponse fromEntity(PayoutBill bill) {

        return PayoutBillResponse.builder()
                .id(bill.getId())
                .shopId(bill.getShopId())
                .billCode(bill.getBillCode())
                .createdAt(bill.getCreatedAt())
                .fromDate(bill.getFromDate())
                .toDate(bill.getToDate())

                .totalGross(bill.getTotalGross())
                .totalPlatformFee(bill.getTotalPlatformFee())
                .totalShippingOrderFee(bill.getTotalShippingOrderFee())
                .totalReturnShippingFee(bill.getTotalReturnShippingFee())
                .totalNetPayout(bill.getTotalNetPayout())

                .status(bill.getStatus().name())
                .transferReference(bill.getTransferReference())
                .receiptImageUrl(bill.getReceiptImageUrl())
                .adminNote(bill.getAdminNote())

                .items(
                        bill.getItems().stream()
                                .map(PayoutBillItemResponse::fromEntity)
                                .toList()
                )
                .shippingOrders(
                        bill.getShippingOrders().stream()
                                .map(PayoutShippingOrderFeeResponse::fromEntity)
                                .toList()
                )
                .returnFees(
                        bill.getReturnShipFees().stream()
                                .map(PayoutReturnShippingFeeResponse::fromEntity)
                                .toList()
                )

                .build();
    }
}
