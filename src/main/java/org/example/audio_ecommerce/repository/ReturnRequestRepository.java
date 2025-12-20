package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Page<ReturnRequest> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ReturnRequest> findByShopId(UUID shopId, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    @Query("select r from ReturnRequest r where r.status = :status and r.updatedAt < :deadline")
    List<ReturnRequest> findUnresponsiveReturns(@Param("status") ReturnStatus status,
                                                @Param("deadline") LocalDateTime deadline);

    Optional<ReturnRequest> findTopByOrderItemIdOrderByCreatedAtDesc(UUID orderItemId);
    List<ReturnRequest> findAllByStatus(ReturnStatus status);


    @Query("""
        select count(r)
        from ReturnRequest r
        where r.shopId = :storeId
          and r.status not in :excluded
          and r.createdAt >= :from
          and r.createdAt <= :to
    """)
    long countValidReturns(
            @Param("storeId") UUID storeId,
            @Param("excluded") Set<ReturnStatus> excluded,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select r.productId, count(r)
        from ReturnRequest r
        where r.shopId = :storeId
          and r.status not in :excluded
          and r.createdAt >= :from
          and r.createdAt <= :to
        group by r.productId
        order by count(r) desc
    """)
    List<Object[]> topReturnedProducts(
            @Param("storeId") UUID storeId,
            @Param("excluded") Set<ReturnStatus> excluded,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}
