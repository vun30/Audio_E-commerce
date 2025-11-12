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

    @Query("""
        select a from DeliveryAssignment a
        where a.deliveryStaff.id = :staffId
          and a.storeOrder.store.storeId = :storeId
          and (:status is null or a.storeOrder.status = :status)
    """)
    Page<DeliveryAssignment> findPageByStoreAndDeliveryStaffAndStatus(
            @Param("storeId") UUID storeId,
            @Param("staffId") UUID staffId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    // Liệt kê tất cả assignment của 1 staff (giữ nguyên theo store để tránh lộ chéo)
    @Query("""
        select a from DeliveryAssignment a
        where a.deliveryStaff.id = :staffId
          and a.storeOrder.store.storeId = :storeId
        order by coalesce(a.pickUpAt, a.assignedAt) desc
    """)
    List<DeliveryAssignment> findAllByStoreAndDeliveryStaff(
            @Param("storeId") UUID storeId,
            @Param("staffId") UUID staffId
    );

    @Query("""
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where so.store.storeId = :storeId
""")
    List<DeliveryAssignment> findAllByStoreIdFetchItems(@Param("storeId") UUID storeId);

    @Query("""
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where so.store.storeId = :storeId
      and so.status = :status
""")
    List<DeliveryAssignment> findAllByStoreIdAndStatusFetchItems(@Param("storeId") UUID storeId,
                                                                 @Param("status") OrderStatus status);

    @Query(value = """
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where so.store.storeId = :storeId
      and (:status is null or so.status = :status)
""",
            countQuery = """
    select count(da) from DeliveryAssignment da
    join da.storeOrder so
    where so.store.storeId = :storeId
      and (:status is null or so.status = :status)
""")
    Page<DeliveryAssignment> findPageByStoreIdAndStatusFetchItems(@Param("storeId") UUID storeId,
                                                                  @Param("status") OrderStatus status,
                                                                  Pageable pageable);

    @Query("""
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where so.store.storeId = :storeId and da.deliveryStaff.id = :staffId
""")
    List<DeliveryAssignment> findAllByStoreAndDeliveryStaffFetchItems(@Param("storeId") UUID storeId,
                                                                      @Param("staffId") UUID staffId);

    @Query(value = """
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where so.store.storeId = :storeId
      and da.deliveryStaff.id = :staffId
      and (:status is null or so.status = :status)
""",
            countQuery = """
    select count(da) from DeliveryAssignment da
    join da.storeOrder so
    where so.store.storeId = :storeId
      and da.deliveryStaff.id = :staffId
      and (:status is null or so.status = :status)
""")
    Page<DeliveryAssignment> findPageByStoreAndDeliveryStaffAndStatusFetchItems(@Param("storeId") UUID storeId,
                                                                                @Param("staffId") UUID staffId,
                                                                                @Param("status") OrderStatus status,
                                                                                Pageable pageable);

    @Query("""
    select da from DeliveryAssignment da
    join fetch da.storeOrder so
    left join fetch so.items si
    where da.id = :assignmentId
""")
    Optional<DeliveryAssignment> findByIdFetchItems(@Param("assignmentId") UUID assignmentId);
}

