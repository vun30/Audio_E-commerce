package org.example.audio_ecommerce.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderData {
    private String email;
    private String orderCode;
    private double totalAmount;
}