package org.example.audio_ecommerce.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreData {
    private String email;
    private String ownerName;
    private String storeName;
}
