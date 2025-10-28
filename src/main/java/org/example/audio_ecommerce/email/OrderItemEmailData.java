package org.example.audio_ecommerce.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEmailData {
    private String name;
    private int quantity;
    private String unitPrice;
    private String lineTotal;
}