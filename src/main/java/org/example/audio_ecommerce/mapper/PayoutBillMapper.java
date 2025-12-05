package org.example.audio_ecommerce.mapper;

import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import java.util.stream.Collectors;

public class PayoutBillMapper {

    public static PayoutBillResponse toResponse(PayoutBill b) {

        return PayoutBillResponse.builder()
                .id(b.getId())
                .shopId(b.getShopId())
                .billCode(b.getBillCode())
                .createdAt(b.getCreatedAt())
                .fromDate(b.getFromDate())
                .toDate(b.getToDate())
                .totalGross(b.getTotalGross())
                .totalPlatformFee(b.getTotalPlatformFee())
                .totalShippingOrderFee(b.getTotalShippingOrderFee())
                .totalReturnShippingFee(b.getTotalReturnShippingFee())
                .totalNetPayout(b.getTotalNetPayout())
                .status(b.getStatus().name())
                .transferReference(b.getTransferReference())
                .receiptImageUrl(b.getReceiptImageUrl())
                .adminNote(b.getAdminNote())
                .items(b.getItems().stream().map(PayoutBillMapper::toItem).collect(Collectors.toList()))
                .shippingOrders(b.getShippingOrders().stream().map(PayoutBillMapper::toShippingFee).collect(Collectors.toList()))
                .returnFees(b.getReturnShipFees().stream().map(PayoutBillMapper::toReturnFee).collect(Collectors.toList()))
                .build();
    }

    private static PayoutBillItemResponse toItem(PayoutBillItem i) {
        return PayoutBillItemResponse.builder()
                .orderItemId(i.getOrderItemId())
                .storeOrderId(i.getStoreOrderId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .finalLineTotal(i.getFinalLineTotal())
                .platformFeePercentage(i.getPlatformFeePercentage())
                .platformFeeAmount(i.getPlatformFeeAmount())
                .netPayout(i.getNetPayout())
                .isReturned(i.getIsReturned())
                .build();
    }

    private static PayoutShippingOrderFeeResponse toShippingFee(PayoutShippingOrderFee s) {
        return PayoutShippingOrderFeeResponse.builder()
                .storeOrderId(s.getStoreOrderId())
                .ghnOrderCode(s.getGhnOrderCode())
                .shippingFee(s.getShippingFee())
                .shippingType(s.getShippingType())
                .build();
    }

    private static PayoutReturnShippingFeeResponse toReturnFee(PayoutReturnShippingFee s) {
        return PayoutReturnShippingFeeResponse.builder()
                .returnRequestId(s.getReturnRequestId())
                .ghnOrderCode(s.getGhnOrderCode())
                .shippingFee(s.getShippingFee())
                .chargedToShop(s.getChargedToShop())
                .shippingType(s.getShippingType())
                .build();
    }
}
