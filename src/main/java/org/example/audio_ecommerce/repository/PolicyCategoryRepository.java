package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyCategoryRepository extends JpaRepository<PolicyCategory, UUID> {

    @Query("SELECT pc FROM PolicyCategory pc WHERE pc.isActive = true ORDER BY pc.displayOrder ASC, pc.createdAt DESC")
    List<PolicyCategory> findAllActiveOrderByDisplayOrder();

    @Query("SELECT pc FROM PolicyCategory pc LEFT JOIN FETCH pc.policyItems WHERE pc.id = :id AND pc.isActive = true")
    Optional<PolicyCategory> findByIdWithItems(UUID id);
}


