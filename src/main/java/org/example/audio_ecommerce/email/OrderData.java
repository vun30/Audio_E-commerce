package org.example.audio_ecommerce.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderData {
    private String email;
    private String customerName;
    private String orderCode;
    private String totalAmount;
    private String paidAt;
    private String receiverName;
    private String shippingAddress;
    private String phoneNumber;
    private String shippingNote;
    private List<OrderItemEmailData> items;
}