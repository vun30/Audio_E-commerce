package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.DeliveryAssignment;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {
    Optional<DeliveryAssignment> findByStoreOrder_Id(UUID storeOrderId);
    // Tất cả assignment của 1 store
    @Query("""
        select a
        from DeliveryAssignment a
        join a.storeOrder so
        where so.store.storeId = :storeId
        order by a.assignedAt desc
        """)
    List<DeliveryAssignment> findAllByStoreId(@Param("storeId") UUID storeId);

    // Có phân trang
    @Query("""
        select a
        from DeliveryAssignment a
        join a.storeOrder so
        where so.store.storeId = :storeId
        """)
    Page<DeliveryAssignment> findPageByStoreId(@Param("storeId") UUID storeId, Pageable pageable);

    // Lọc theo trạng thái StoreOrder
    @Query("""
        select a
        from DeliveryAssignment a
        join a.storeOrder so
        where so.store.storeId = :storeId
          and so.status = :status
        order by a.assignedAt desc
        """)
    List<DeliveryAssignment> findAllByStoreIdAndStatus(@Param("storeId") UUID storeId,
                                                       @Param("status") OrderStatus status);

    // Lọc + phân trang
    @Query("""
        select a
        from DeliveryAssignment a
        join a.storeOrder so
        where so.store.storeId = :storeId
          and (:status is null or so.status = :status)
        """)
    Page<DeliveryAssignment> findPageByStoreIdAndStatus(@Param("storeId") UUID storeId,
                                                        @Param("status") OrderStatus status,
                                                        Pageable pageable);
}

