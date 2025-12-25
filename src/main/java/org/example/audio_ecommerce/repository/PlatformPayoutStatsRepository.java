//package org.example.audio_ecommerce.repository;
//
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.Repository;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface PlatformPayoutStatsRepository extends Repository<Object, UUID> {
//
//    @Query(value = """
//        SELECT
//            COALESCE(COUNT(i.id), 0)                                              AS eligibleItemCount,
//            COALESCE(COUNT(DISTINCT o.id), 0)                                     AS eligibleOrderCount,
//            COALESCE(SUM(i.final_line_total), 0)                                  AS eligibleGross,
//            COALESCE(SUM(i.platform_fee_amount), 0)                               AS platformFeeCollected
//        FROM store_order_item i
//        JOIN store_order o ON o.id = i.store_order_id
//        WHERE o.delivered_at IS NOT NULL
//          AND o.delivered_at >= :from
//          AND o.delivered_at <  :to
//
//          -- item eligible payout
//          AND i.eligible_for_payout = 1
//          AND i.is_payout = 1
//
//          -- optional filter stores
//          AND (:applyStoreFilter = 0 OR o.store_id IN (:storeIds))
//        """, nativeQuery = true)
//    Object[] payoutRevenueStats(
//            @Param("from") String from,
//            @Param("to") String to,
//            @Param("applyStoreFilter") int applyStoreFilter,
//            @Param("storeIds") List<UUID> storeIds
//    );
//}
