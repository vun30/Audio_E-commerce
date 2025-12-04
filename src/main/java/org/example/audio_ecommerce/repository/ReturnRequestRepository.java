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
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Page<ReturnRequest> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ReturnRequest> findByShopId(UUID shopId, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    @Query("select r from ReturnRequest r where r.status = :status and r.updatedAt < :deadline")
    List<ReturnRequest> findUnresponsiveReturns(@Param("status") ReturnStatus status,
                                                @Param("deadline") LocalDateTime deadline);
}
