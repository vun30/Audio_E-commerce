package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StoreGroup {
    private UUID storeId;                 // có thể null nếu chưa gán
    private String storeName;             // tùy bạn map thêm
    private List<CartItemResponse> items; // các item thuộc shop
}
