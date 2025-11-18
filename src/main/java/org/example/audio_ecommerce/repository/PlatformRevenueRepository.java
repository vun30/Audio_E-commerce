package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformRevenue;
import org.example.audio_ecommerce.repository.projection.PlatformRevenueAgg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlatformRevenueRepository extends JpaRepository<PlatformRevenue, UUID> {

    Page<PlatformRevenue> findByRevenueDateBetween(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    @Query("""
           SELECT pr.type AS type,
                  COALESCE(SUM(pr.amount), 0) AS totalAmount
           FROM PlatformRevenue pr
           WHERE pr.revenueDate BETWEEN :fromDate AND :toDate
           GROUP BY pr.type
           """)
    List<PlatformRevenueAgg> aggregateByTypeAndDate(LocalDate fromDate, LocalDate toDate);
}
