package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductReviewReplyRepository extends JpaRepository<ProductReviewReply, UUID> {
}
