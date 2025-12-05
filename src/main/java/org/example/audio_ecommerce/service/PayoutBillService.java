package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;
import org.example.audio_ecommerce.entity.PayoutBill;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PayoutBillService {

    /**
     * Tạo Payout Bill đầy đủ cho 1 shop:
     * - Build PayoutBillItem
     * - Build PayoutShippingOrderFee
     * - Build PayoutReturnShippingFee
     * - Tính total
     * - Lưu bill
     */
    PayoutBill createBillForShop(UUID storeId);


    /**
     * Admin xác nhận đã thanh toán bill:
     * - set transferReference
     * - set receiptImageUrl
     * - set adminNote
     * - set status = PAID
     * - update trạng thái paidByShop & isPayout của các bảng liên quan
     */
    PayoutBill markBillAsPaid(UUID billId,
                              String transferReference,
                              String receiptImageUrl,
                              String adminNote);


    /**
     * Lấy full bill:
     * - bill
     * - bill items
     * - shipping fees
     * - return shipping fees
     */
    PayoutBill getFullBill(UUID billId);

    List<PayoutBill> autoCreateBillsForAllStores();

    PayoutBill getOrCreateBillForStore(UUID storeId);

    List<PayoutBill> listBills(UUID storeId,
                               PayoutBillStatus status,
                               LocalDateTime fromDate,
                               LocalDateTime toDate,
                               String billCode);



}