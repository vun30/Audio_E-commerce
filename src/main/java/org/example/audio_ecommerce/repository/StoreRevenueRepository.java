package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreRevenue;
import org.example.audio_ecommerce.repository.projection.StoreRevenueAgg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

public interface StoreRevenueRepository extends JpaRepository<StoreRevenue, UUID> {

    Page<StoreRevenue> findByStoreIdAndRevenueDateBetween(
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    @Query("""
           SELECT 
             COALESCE(SUM(sr.amount), 0)         AS totalAmount,
             COALESCE(SUM(sr.feePlatform), 0)    AS totalPlatformFee,
             COALESCE(SUM(sr.feeShipping), 0)    AS totalShippingFee
           FROM StoreRevenue sr
           WHERE sr.storeId = :storeId
             AND sr.revenueDate BETWEEN :fromDate AND :toDate
           """)
    StoreRevenueAgg aggregateByStoreAndDate(UUID storeId, LocalDate fromDate, LocalDate toDate);
}
