package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

//    // 🔍 Tìm category theo tên
//    Optional<Category> findByName(String name);
//
//    // 🔍 Kiểm tra tồn tại theo tên (để tránh tạo trùng)
//    boolean existsByName(String name);
//
//    // 🔍 Lấy tất cả category sắp xếp theo thứ tự sortOrder tăng dần
//    List<Category> findAllByOrderBySortOrderAsc();
//
//    // 🔍 Tìm category có tên chứa từ khóa (cho tìm kiếm trong admin panel)
//    List<Category> findByNameContainingIgnoreCase(String keyword);
//    Optional<Category> findByNameIgnoreCase(String name);

      // Kiểm tra xem có category con không
    boolean existsByParent(Category parent);

    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
    FROM Product p
    JOIN p.categories c
    WHERE c = :category
""")
boolean existsByCategory(@Param("category") Category category);

    Optional<Category> findByNameIgnoreCase(String name);

boolean existsByNameIgnoreCase(String name);

    @Query("""
        select c.categoryId
        from Category c
        where c.name = :name
    """)
    Optional<UUID> findIdByName(@Param("name") String name);

}
