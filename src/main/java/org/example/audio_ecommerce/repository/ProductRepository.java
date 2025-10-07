package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
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

    // ✅ Lấy sản phẩm theo khoảng giá
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") double minPrice,
                                   @Param("maxPrice") double maxPrice,
                                   Pageable pageable);
}
