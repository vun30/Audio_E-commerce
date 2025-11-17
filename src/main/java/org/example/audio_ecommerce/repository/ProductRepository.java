package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // ✅ Tìm sản phẩm theo SKU
    Optional<Product> findBySku(String sku);

    // ✅ Tìm theo slug
    Optional<Product> findBySlug(String slug);

    // ✅ Lấy toàn bộ sản phẩm theo trạng thái (ACTIVE, INACTIVE, ...)
    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    // ✅ Tìm kiếm sản phẩm gần đúng theo tên (giống Google)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // ✅ Lọc theo Category
    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId")
    Page<Product> findAllByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    // ✅ Lọc theo Store
    @Query("SELECT p FROM Product p WHERE p.store.storeId = :storeId")
    Page<Product> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);

    // ✅ Lọc sản phẩm nổi bật (isFeatured = true)
    Page<Product> findAllByIsFeaturedTrue(Pageable pageable);

    // ✅ Lấy sản phẩm theo nhiều ID
    List<Product> findAllByProductIdIn(List<UUID> productIds);

      /**
     * 🔍 Kiểm tra SKU có bị trùng trong cùng một cửa hàng hay không
     */
    boolean existsByStore_StoreIdAndSku(UUID storeId, String sku);

    /**
     * 🔍 Kiểm tra slug đã tồn tại hay chưa (đảm bảo slug duy nhất toàn hệ thống)
     */
    boolean existsBySlug(String slug);

    // ✅ Lấy sản phẩm theo khoảng giá
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") double minPrice,
                                   @Param("maxPrice") double maxPrice,
                                   Pageable pageable);

    long countByStore_StoreIdAndStatus(UUID storeId, ProductStatus status);


     @Query("""
        SELECT p FROM Product p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:storeId IS NULL OR p.store.storeId = :storeId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Product> findAllWithFilters(
            @Param("status") String status,
            @Param("categoryId") UUID categoryId,
            @Param("storeId") UUID storeId,
            @Param("keyword") String keyword,
            Pageable pageable);

     @Query("""
SELECT DISTINCT p FROM Product p
JOIN p.store s
LEFT JOIN s.storeAddresses addr
WHERE (:status IS NULL OR p.status = :status)
  AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
  AND (:storeId IS NULL OR s.storeId = :storeId)
  AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
  AND (:provinceCode IS NULL OR addr.provinceCode = :provinceCode)
  AND (:districtCode IS NULL OR addr.districtCode = :districtCode)
  AND (:wardCode IS NULL OR addr.wardCode = :wardCode)
""")
Page<Product> findAllWithAdvancedFilters(
        @Param("status") String status,
        @Param("categoryId") UUID categoryId,
        @Param("storeId") UUID storeId,
        @Param("keyword") String keyword,
        @Param("provinceCode") String provinceCode,
        @Param("districtCode") String districtCode,
        @Param("wardCode") String wardCode,
        Pageable pageable
);

     @Query("SELECT p.store FROM Product p WHERE p.productId = :productId")
Optional<Store> findStoreByProductId(UUID productId);

}
