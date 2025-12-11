package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {

    // Lấy toàn bộ thuộc tính của 1 category
    List<CategoryAttribute> findAllByCategory_CategoryId(UUID categoryId);

    // Lấy thuộc tính theo tên (nếu muốn check trùng)
    boolean existsByCategory_CategoryIdAndAttributeNameIgnoreCase(UUID categoryId, String attributeName);

    // Tìm 1 attribute cụ thể
    CategoryAttribute findByCategory_CategoryIdAndAttributeId(UUID categoryId, UUID attributeId);
}
