package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<Product, UUID> {

    List<Product> findDistinctByCategoriesIn(List<Category> categories);

    List<Product> findByCategories_CategoryId(UUID categoryId);
}
