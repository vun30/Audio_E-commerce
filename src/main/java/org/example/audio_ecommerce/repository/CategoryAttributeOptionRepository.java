package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CategoryAttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryAttributeOptionRepository extends JpaRepository<CategoryAttributeOption, UUID> {

    // Lấy tất cả option theo attributeId
    List<CategoryAttributeOption> findAllByAttribute_AttributeId(UUID attributeId);

    // Xóa toàn bộ option theo attributeId
    void deleteAllByAttribute_AttributeId(UUID attributeId);
}
