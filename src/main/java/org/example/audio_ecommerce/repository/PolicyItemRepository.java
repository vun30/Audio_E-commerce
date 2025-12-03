package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PolicyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyItemRepository extends JpaRepository<PolicyItem, UUID> {

    @Query("SELECT pi FROM PolicyItem pi WHERE pi.policyCategory.id = :categoryId " +
           "AND pi.isActive = true ORDER BY pi.displayOrder ASC, pi.createdAt DESC")
    List<PolicyItem> findActiveByCategoryIdOrderByDisplayOrder(@Param("categoryId") UUID categoryId);

    @Query("SELECT pi FROM PolicyItem pi WHERE pi.policyCategory.id = :categoryId " +
           "ORDER BY pi.displayOrder ASC, pi.createdAt DESC")
    List<PolicyItem> findByCategoryIdOrderByDisplayOrder(@Param("categoryId") UUID categoryId);

    @Query("SELECT COUNT(pi) FROM PolicyItem pi WHERE pi.policyCategory.id = :categoryId AND pi.isActive = true")
    long countActiveByCategoryId(@Param("categoryId") UUID categoryId);
}

