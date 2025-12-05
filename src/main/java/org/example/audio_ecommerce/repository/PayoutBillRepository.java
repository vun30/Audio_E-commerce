package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;
import org.example.audio_ecommerce.entity.PayoutBill;
import org.example.audio_ecommerce.entity.PayoutBillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PayoutBillRepository extends JpaRepository<PayoutBill, UUID> {


    PayoutBill findFirstByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, PayoutBillStatus status);

    @Query("""
        SELECT b FROM PayoutBill b
        WHERE (:storeId IS NULL OR b.shopId = :storeId)
        AND (:status IS NULL OR b.status = :status)
        AND (:fromDate IS NULL OR b.createdAt >= :fromDate)
        AND (:toDate IS NULL OR b.createdAt <= :toDate)
        AND (:billCode IS NULL OR LOWER(b.billCode) LIKE LOWER(CONCAT('%', :billCode, '%')))
        ORDER BY b.createdAt DESC
        """)
List<PayoutBill> filterBills(UUID storeId,
                             PayoutBillStatus status,
                             LocalDateTime fromDate,
                             LocalDateTime toDate,
                             String billCode);

    boolean existsByShopIdAndStatusNot(UUID shopId, PayoutBillStatus status);

}
