package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    // 🔎 Lấy tất cả biến thể của 1 sản phẩm
    List<ProductVariantEntity> findAllByProduct_ProductId(UUID productId);

    // 🔎 Xóa tất cả biến thể theo productId
    void deleteAllByProduct_ProductId(UUID productId);

    // 🔎 Kiểm tra biến thể có tồn tại hay không
    boolean existsById(UUID id);
}
