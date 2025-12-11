package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {

    // Lấy tất cả attribute value của 1 product
    List<ProductAttributeValue> findAllByProduct_ProductId(UUID productId);

    // Xóa tất cả attribute value của 1 product
    void deleteAllByProduct_ProductId(UUID productId);

    // Tìm theo attribute
    List<ProductAttributeValue> findAllByAttribute_AttributeId(UUID attributeId);
}
