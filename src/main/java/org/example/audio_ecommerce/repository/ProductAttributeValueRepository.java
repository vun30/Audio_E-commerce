package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {

    // Lấy tất cả attribute value của 1 product
    List<ProductAttributeValue> findAllByProduct_ProductId(UUID productId);

    // Xóa tất cả attribute value của 1 product
    void deleteAllByProduct_ProductId(UUID productId);

    // Tìm theo attribute
    List<ProductAttributeValue> findAllByAttribute_AttributeId(UUID attributeId);

    // ✅ UPSERT: lấy các value đã có của product theo attributeId
    @Query("""
        select pav
        from ProductAttributeValue pav
        where pav.product.productId = :productId
          and pav.attribute.attributeId in :attrIds
    """)
    List<ProductAttributeValue> findByProductIdAndAttrIds(
            @Param("productId") UUID productId,
            @Param("attrIds") List<UUID> attrIds
    );

    // ✅ SEARCH: lấy value của ACTIVE products theo category + attributeName
    @Query("""
        select pav
        from ProductAttributeValue pav
        join pav.product p
        join p.categories c
        join pav.attribute a
        where p.status = org.example.audio_ecommerce.entity.Enum.ProductStatus.ACTIVE
          and c.categoryId = :categoryId
          and a.attributeName in :attrNames
          and pav.value is not null
    """)
    List<ProductAttributeValue> findForSimilarity(
            @Param("categoryId") UUID categoryId,
            @Param("attrNames") List<String> attrNames
    );
}
