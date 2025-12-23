package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByAccount(Account account);

    // ✅ Sửa lại: dùng field "id" của Account kế thừa từ BaseEntity
    Optional<Store> findByAccount_Id(UUID accountId);

    boolean existsByAccount_Id(UUID accountId);

    // 🔍 Tìm store theo email tài khoản (hay dùng cho login dashboard store)
    Optional<Store> findByAccount_Email(String email);

    // 🔍 Lấy danh sách tất cả store theo trạng thái (INACTIVE, PENDING, ACTIVE, ...)
    List<Store> findAllByStatus(StoreStatus status);

    // 🔍 Kiểm tra xem store đã tồn tại theo tên (để tránh tạo trùng tên)
    boolean existsByStoreName(String storeName);

    // 🔍 Lấy store theo ID nhưng load luôn account (nếu cần)
    Optional<Store> findByStoreId(UUID storeId);

    Page<Store> findByStoreNameContainingIgnoreCase(String keyword, Pageable pageable);

    // ✅ (Tuỳ chọn) tìm theo ID và trạng thái — dùng nếu muốn check nhanh
    Optional<Store> findByStoreIdAndStatus(UUID storeId, StoreStatus status);

    @Query("SELECT p.store FROM Product p WHERE p.productId = :productId")
    Optional<Store> findStoreByProductId(UUID productId);

    @Query("SELECT s.storeId FROM Store s")
    List<UUID> findAllStoreIds();

    @Query("""
        select s
        from Store s
        join fetch s.wallet w
        where s.status = :status
          and s.legalPoint is not null
    """)
    List<Store> findAllActiveWithWallet(@Param("status") StoreStatus status);


    @Query("""
    select distinct s
    from Store s
    join fetch s.wallet w
    left join fetch s.account a
    where s.status in :statuses
""")
    List<Store> findStoresWithWalletByStatuses(@Param("statuses") List<StoreStatus> statuses);
    List<Store> findByStatus(StoreStatus status);

    @Query(value = """
        select count(*)
        from stores s
        where s.created_at >= :from and s.created_at < :to
    """, nativeQuery = true)
    long countNewStoresInRange(@Param("from") String from, @Param("to") String to);

    @Query(value = """
        select month(s.created_at) as m, count(*) as cnt
        from stores s
        where year(s.created_at) = :year
        group by month(s.created_at)
    """, nativeQuery = true)
    java.util.List<Object[]> countNewStoresByMonth(@Param("year") int year);

}
