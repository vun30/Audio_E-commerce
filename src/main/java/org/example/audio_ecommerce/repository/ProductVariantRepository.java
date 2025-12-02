package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    // ============================================================
    // 📌 LẤY DANH SÁCH BIẾN THỂ CỦA SẢN PHẨM
    // ============================================================
    List<ProductVariantEntity> findAllByProduct_ProductId(UUID productId);

    // ============================================================
    // 📌 XOÁ TẤT CẢ BIẾN THỂ THEO productId
    // ============================================================
    void deleteAllByProduct_ProductId(UUID productId);

    // ============================================================
    // 📌 KIỂM TRA BIẾN THỂ CÓ TỒN TẠI KHÔNG
    // ============================================================
    boolean existsById(UUID id);

    // ============================================================
    // 📌 SHOPEE LOGIC — CHECK SKU TRÙNG TRONG CÙNG 1 PRODUCT
    // ============================================================
    boolean existsByProduct_ProductIdAndVariantSku(UUID productId, String variantSku);

    // ============================================================
    // 📌 SHOPEE LOGIC — CHECK SKU TRÙNG TRONG 1 PRODUCT (EXCLUDE ID)
    //   -> Dùng khi update variant
    // ============================================================
    boolean existsByProduct_ProductIdAndVariantSkuAndIdNot(UUID productId, String variantSku, UUID variantId);

    // ============================================================
    // 📌 LẤY 1 VARIANT THEO PRODUCT + SKU
    // ============================================================
    Optional<ProductVariantEntity> findByProduct_ProductIdAndVariantSku(UUID productId, String variantSku);

    // ============================================================
    // 📌 LẤY 1 VARIANT THEO PRODUCT + ID
    // ============================================================
    Optional<ProductVariantEntity> findByIdAndProduct_ProductId(UUID variantId, UUID productId);

    // ============================================================
    // 📌 XOÁ BIẾN THỂ THEO DANH SÁCH ID
    // ============================================================
    void deleteAllByIdIn(List<UUID> ids);

    // ============================================================
    // 📌 ĐẾM SỐ BIẾN THỂ CỦA PRODUCT
    // ============================================================
    long countByProduct_ProductId(UUID productId);

    // ============================================================
    // 📌 KIỂM TRA 1 VARIANT CÓ THUỘC PRODUCT KHÔNG
    // ============================================================
    boolean existsByIdAndProduct_ProductId(UUID variantId, UUID productId);

    // ============================================================
    // 📌 LẤY DANH SÁCH VARIANT THEO LIST ID (dùng cho update/delete)
    // ============================================================
    List<ProductVariantEntity> findAllByIdIn(List<UUID> ids);
}
