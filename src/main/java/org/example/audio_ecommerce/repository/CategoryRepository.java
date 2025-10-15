package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 🔍 Tìm category theo tên
    Optional<Category> findByName(String name);

    // 🔍 Tìm category theo slug (thường dùng cho SEO hoặc URL)
    Optional<Category> findBySlug(String slug);

    // 🔍 Kiểm tra tồn tại theo tên (để tránh tạo trùng)
    boolean existsByName(String name);

    // 🔍 Lấy tất cả category sắp xếp theo thứ tự sortOrder tăng dần
    List<Category> findAllByOrderBySortOrderAsc();

    // 🔍 Tìm category có tên chứa từ khóa (cho tìm kiếm trong admin panel)
    List<Category> findByNameContainingIgnoreCase(String keyword);
    Optional<Category> findByNameIgnoreCase(String name);
}
