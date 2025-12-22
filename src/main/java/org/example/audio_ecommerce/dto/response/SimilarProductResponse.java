package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SimilarProductResponse {
    private List<UUID> productIds;
}