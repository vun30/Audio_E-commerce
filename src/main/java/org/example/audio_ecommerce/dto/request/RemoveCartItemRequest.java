package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RemoveCartItemRequest {
    // cho phép xóa 1 hoặc nhiều item
    private List<UUID> cartItemIds;
}