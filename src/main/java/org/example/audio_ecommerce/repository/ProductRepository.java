package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // =============================
    // BASIC FINDERS
    // =============================
//    Optional<Product> findBySku(String sku);
//    Optional<Product> findBySlug(String slug);
//
//    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByStore_StoreIdAndSku(UUID storeId, String sku);
    boolean existsBySlug(String slug);

    long countByStore_StoreIdAndStatus(UUID storeId, ProductStatus status);

    // =============================
    // MANY-TO-MANY CATEGORY QUERIES
    // =============================

//    /** Lấy product theo categoryId */
//    Page<Product> findByCategories_CategoryId(UUID categoryId, Pageable pageable);
//
//    /** Kiểm tra category có đang được sử dụng không */
//    boolean existsByCategories(Category category);
//
//    /** Kiểm tra bằng categoryId */
//    boolean existsByCategories_CategoryId(UUID categoryId);
//
//    /** Lấy tất cả theo danh sách ID */
//    List<Product> findAllByProductIdIn(List<UUID> productIds);

    // =============================
    // PRICE
    // =============================
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice,
            Pageable pageable
    );

    // =============================
    // FILTER BASIC (status + store + category + keyword)
    // =============================
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.categories c
        WHERE (:status IS NULL OR p.status = :status)
          AND (:categoryId IS NULL OR c.categoryId = :categoryId)
          AND (:storeId IS NULL OR p.store.storeId = :storeId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Product> findAllWithFilters(
            @Param("status") ProductStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("storeId") UUID storeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =============================
    // ADVANCED FILTER (STORE ADDRESS + CATEGORY)
    // =============================
@Query("""
SELECT DISTINCT p FROM Product p
JOIN p.store s
LEFT JOIN s.storeAddresses addr
LEFT JOIN p.categories c
WHERE (:status IS NULL OR p.status = :status)
  AND (:categoryId IS NULL OR c.categoryId = :categoryId)
  AND (:storeId IS NULL OR s.storeId = :storeId)
  AND (:keyword IS NULL 
       OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%'))
  )
  AND (:provinceCode IS NULL OR addr.provinceCode = :provinceCode)
  AND (:districtCode IS NULL OR addr.districtCode = :districtCode)
  AND (:wardCode IS NULL OR addr.wardCode = :wardCode)
""")
Page<Product> findAllWithAdvancedFilters(
        @Param("status") ProductStatus status,
        @Param("categoryId") UUID categoryId,
        @Param("storeId") UUID storeId,
        @Param("keyword") String keyword,
        @Param("provinceCode") String provinceCode,
        @Param("districtCode") String districtCode,
        @Param("wardCode") String wardCode,
        Pageable pageable
);



    // =============================
    // STORE UTIL
    // =============================
    @Query("SELECT p.store FROM Product p WHERE p.productId = :productId")
    Optional<Store> findStoreByProductId(UUID productId);

    @Modifying
    @Query("UPDATE Product p SET p.status = 'SUSPENDED' WHERE p.store.storeId = :storeId")
    int suspendAllProductsByStore(UUID storeId);

    @Modifying
    @Query("UPDATE Product p SET p.status = 'UNLISTED' WHERE p.store.storeId = :storeId")
    int unlistAllProductsByStore(UUID storeId);

    @Modifying
    @Query("UPDATE Product p SET p.status = 'ACTIVE' WHERE p.store.storeId = :storeId")
    int activateAllProductsByStore(UUID storeId);

    boolean existsByCategoriesContaining(Category category);


    // =============================
    // CATEGORY ATTRIBUTES (không dùng nữa)
    // =============================
    // REMOVE: List<CategoryAttribute> findAllByCategory_CategoryId(UUID categoryId);
}
